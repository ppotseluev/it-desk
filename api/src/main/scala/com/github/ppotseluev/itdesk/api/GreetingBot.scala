package com.github.ppotseluev.itdesk.api

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import com.github.ppotseluev.itdesk.bots.core.BotDsl._

import java.time.LocalDateTime

class GreetingBot[F[_]: Sync] {

  private def start = Node[F]("start", reply("Привет"))
  private def about = Node[F]("about", reply("Я телеграм-бот!"))
  private def about2 = Node[F](
    "about2",
    reply("Я умею показывать время :)")
  )
  private val getTime = execute(
    Sync[F].delay {
      LocalDateTime.now()
    }
  )
  private val showTime = Node(
    "show_time",
    getTime.flatMap(t => reply(t.toString))
  )

  private val graph: BotGraph[F] =
    Graph(
      start ~> about by "Show info",
      about ~> start by "Назад",
      about ~> about2 by "Что ты умеешь?",
      about2 ~> about by "Назад",
      about2 ~> start by "В начало",
      about2 ~> showTime by "Давай!",
      showTime ~> about2 by "Назад"
    )

  val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
//      "/start" -> goTo(about2.id),
      "/time" -> getTime.map(_.toString).flatMap(reply),
      "/help" -> reply("Here should be some help message")
    )
  )
}

object GreetingBot {
  def apply[F[_]: Sync] = new GreetingBot[F]
}
