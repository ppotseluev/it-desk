package com.github.ppotseluev.itdesk.core.admin

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotCommand
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.BotDsl.reply
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.{AnyInput, CallbackButton, equalTo}
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario.GlobalAction.GoTo
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.core.expert.ExpertService
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend

class AdminBot[F[_]: Sync](implicit
    sttpBackend: SttpBackend[F, Any],
    expertService: ExpertService[F]
) {

  private def saveExpert(username: String): BotScript[F, Unit] = execute {
    expertService.inviteExpert(username).void
  }

  private val selectAction = Node[F]("select_action", reply("Выберите действие"))
  private val askUsername = Node[F]("ask_username", reply("Введите @tg_username"))
  private val expertAdded = Node[F](
    "expert_added",
    getInput[F].flatMap { tgUsername =>
      val username = tgUsername.stripPrefix("@")
      saveExpert(username) >> reply(
        s"Успешно, теперь у @$username есть доступ к @it_desk_expert_bot"
      )
    }
  )

  private val findExpertsScript: BotScript[F, Unit] = execute {
    expertService.getAllExperts
  }.flatMap { experts =>
    if (experts.isEmpty) {
      reply("No experts found")
    } else {
      val commands = experts.map { expert =>
        val name = expert.info.name.getOrElse("UNDEFINED_NAME")
        BotCommand.Callback(name, callbackData = expert.user.tgUserId.toString)
      }
      reply(availableCommands = commands)
    }
  }
  private val showExpertScript: BotScript[F, Unit] =
    for {
      input <- getOrFail("callback_data", _.callbackQuery.flatMap(_.data))
      expert <- execute(expertService.getExpert(input.toLong))
      _ <- reply(expert.show, expert.info.photo.map(_.asRight))
    } yield ()

  private val findExperts = Node[F](
    "find_experts",
    findExpertsScript
  )
  private val showExpert = Node[F](
    "show_expert",
    showExpertScript
  )

  private val graph: BotGraph[F] =
    Graph(
      selectAction ~> askUsername transit equalTo("Выдать доступ эксперту"),
      askUsername ~> selectAction transit (equalTo("Отмена"), 0),
      askUsername ~> expertAdded transit (AnyInput, 1),
      expertAdded ~> selectAction transit equalTo("Ok"),
      findExperts ~> showExpert transit CallbackButton,
      showExpert ~> selectAction transit equalTo("Меню")
    )

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = selectAction.id,
    globalCommands = Map(
      "/show_experts" -> GoTo(findExperts.id),
      "/start" -> GoTo(selectAction.id)
    )
  )

  val logic = new Bot(
    scenario = scenario,
    fallbackPolicy = FallbackPolicy.Ignore
  )
}

object AdminBot {
  def apply[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any], expertDao: ExpertService[F]) =
    new AdminBot[F]
}
