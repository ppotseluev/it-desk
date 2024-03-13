package com.github.ppotseluev.itdesk.core.admin

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.BotDsl.reply
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.core.expert.ExpertService
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend

class AdminBot[F[_]: Sync](implicit
    sttpBackend: SttpBackend[F, Any],
    expertService: ExpertService[F]
) {

  private val start = Node.start[F]
  private val selectAction = Node[F]("select_action", reply("Выберите действие"))
  private val askUsername = Node[F]("ask_username", reply("Введите @tg_nickname"))
  private val expertAdded = Node[F](
    "expert_added",
    getInput[F].flatMap { tgUsername =>
      saveExpert(tgUsername) >> reply(
        s"Успешно, теперь у @$tgUsername есть доступ к @it_desk_expert_bot"
      )
    }
  )

  private def saveExpert(tgUsername: String): BotScript[F, Unit] = execute {
    val username = tgUsername.stripPrefix("@")
    expertService.inviteExpert(username).void
  }

  private val graph: BotGraph[F] =
    Graph(
      start ~> selectAction by "/start",
      selectAction ~> askUsername by "Выдать доступ эксперту",
      askUsername ~> selectAction by ("Отмена", 0),
      askUsername ~> expertAdded byAnyInput 1,
      expertAdded ~> selectAction by "Ok"
    )

  private def showExperts: BotScript[F, Unit] = execute {
    expertService.getAllExperts
  }.flatMap { experts =>
    experts.headOption match { //TODO
      case Some(expert) =>
        val txt = expert.show
        reply(txt, expert.info.photo.map(_.asRight))
      case None => reply("No experts found")
    }
  }

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
      "/show_experts" -> showExperts
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
