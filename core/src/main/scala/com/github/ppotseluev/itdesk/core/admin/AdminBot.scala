package com.github.ppotseluev.itdesk.core.admin

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
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
  private val addExpert = Node[F]("add_expert", reply("Введите @tg_nickname"))
  private val expertAdded = Node[F](
    "expert_added",
    (getInput[F] >>= saveExpert) >> reply("Эксперт добавлен")
  )

  private def saveExpert(tgUsername: String): BotScript[F, Unit] = execute {
    val username = tgUsername.stripPrefix("@")
    expertService.inviteExpert(username).void
  }

  private val graph: BotGraph[F] =
    Graph(
      start ~> selectAction by "/start",
      selectAction ~> addExpert by "Добавить эксперта",
      addExpert ~> selectAction by ("Отмена", 0),
      addExpert ~> expertAdded byAnyInput 1,
      expertAdded ~> selectAction by "Ok"
    )

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map.empty
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
