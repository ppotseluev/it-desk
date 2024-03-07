package com.github.ppotseluev.itdesk.bots.core.scenario

import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.Action.GoTo
import com.github.ppotseluev.itdesk.bots.core.Message.Payload
import com.github.ppotseluev.itdesk.bots.core._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LBase.LEdgeImplicits
import scalax.collection.edge._
import scalax.collection.immutable.Graph

class GraphBotScenario(
    val graph: BotGraph,
    val startFrom: BotStateId,
    val globalCommands: Map[BotCommand, Action]
) {
  import GraphBotScenario.EdgeImplicits._

  private val states: Map[BotStateId, graph.NodeT] =
    graph.nodes
      .map(node => node.id -> node)
      .toMap

  private def extractAvailableCommands(node: graph.NodeT): Seq[BotCommand] =
    node.outgoing.toSeq.sortBy(_.order).flatMap(asCommand)

  private def toBotState(node: graph.NodeT): Option[BotState] =
    node.action match {
      case action: BasicAction =>
        BotState(
          id = node.id,
          action = action,
          availableCommands = extractAvailableCommands(node)
        ).some
      case Action.GoTo(state) =>
        get(state)
    }

  private def asCommand(edge: graph.EdgeT): Option[BotCommand] =
    edge.expectedInputPredicate match {
      case ExpectedInputPredicate.TextIsEqualTo(expectedText) =>
        Some(expectedText)
    }

  private def isMatched(command: Payload)(edge: graph.EdgeT): Boolean =
    Matcher.isMatched(command)(edge.expectedInputPredicate)

  def transit(stateId: BotStateId, command: Message.Payload): Option[BotState] =
    states
      .get(stateId)
      .flatMap(_.outgoing.find(isMatched(command)))
      .map(_.to)
      .flatMap(toBotState)
      .orElse(globalState(stateId, command))

  private def get(stateId: BotStateId): Option[BotState] =
    states.get(stateId).flatMap(toBotState)

  private def globalState(currentStateId: BotStateId, command: Message.Payload): Option[BotState] =
    globalCommands.get(command.text) match {
      case Some(action: BasicAction) => get(currentStateId).map(_.copy(action = action))
      case Some(GoTo(anotherState))  => get(anotherState)
      case None                      => None
    }
}

object GraphBotScenario {
  case class EdgeLabel(order: Int, expectedInputPredicate: ExpectedInputPredicate)
  object EdgeLabel {
    def apply(command: String): EdgeLabel =
      EdgeLabel(0, ExpectedInputPredicate.TextIsEqualTo(command))
  }

  implicit class LDiEdgeAssoc[N](val e: DiEdge[N]) extends AnyVal {
    def by(command: String) = e + EdgeLabel(command)
  }

  case class Node(id: BotStateId, action: Action)

  type BotGraph = Graph[Node, LDiEdge]

  object EdgeImplicits extends LEdgeImplicits[EdgeLabel]

}
