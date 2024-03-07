package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.bots.core.Action
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph

object GreetingBot {

  private val start = Node("start", Action.Reply("Привет"))
  private val about = Node("about", Action.Reply("Я телеграм-бот!"))
  private val about2 = Node(
    "about2",
    Action.Reply("Я пока ничего не умею \uD83D\uDE44. Но я скоро научусь :)")
  )

  private val graph: BotGraph =
    Graph(
      start ~> about by "Show info",
      about ~> start by "Назад",
      about ~> about2 by "Что ты умеешь?",
      about2 ~> about by "Назад",
      about2 ~> start by "В начало"
    )

  val scenario: GraphBotScenario = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
      "/start" -> Action.GoTo(start.id),
      "/help" -> Action.Reply("Here should be some help message")
    )
  )
}
