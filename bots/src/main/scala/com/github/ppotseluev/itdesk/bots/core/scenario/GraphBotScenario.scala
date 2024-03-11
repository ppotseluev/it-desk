package com.github.ppotseluev.itdesk.bots.core.scenario

import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotDsl.BotScript
import com.github.ppotseluev.itdesk.bots.core._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import scalax.collection.GraphEdge
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LBase.LEdgeImplicits
import scalax.collection.edge._
import scalax.collection.immutable.Graph

class GraphBotScenario[F[_]](
    val graph: BotGraph[F],
    val startFrom: BotStateId,
    val globalCommands: Map[BotCommand, BotScript[F, Unit]]
) {
  import GraphBotScenario.EdgeImplicits._

  private val states: Map[BotStateId, graph.NodeT] =
    graph.nodes
      .map(node => node.id -> node)
      .toMap

  private def extractAvailableCommands(node: graph.NodeT): List[BotCommand] =
    node.outgoing.toList.sortBy(_.order).flatMap(asCommand)

  private def toBotState(node: graph.NodeT): BotState[F] =
    BotState(
      id = node.id,
      action = node.action,
      availableCommands = extractAvailableCommands(node)
    )

  private def asCommand(edge: graph.EdgeT): Option[BotCommand] =
    edge.expectedInputPredicate match {
      case ExpectedInputPredicate.TextIsEqualTo(expectedText) =>
        Some(expectedText)
      case ExpectedInputPredicate.AnyInput =>
        None
    }

  private def isMatched(command: String)(edge: graph.EdgeT): Boolean =
    Matcher.isMatched(command)(edge.expectedInputPredicate)

  def transit(stateId: BotStateId, command: String): Option[BotState[F]] =
    states
      .get(stateId)
      .flatMap(_.outgoing.find(isMatched(command)))
      .map(_.to)
      .map(toBotState)
      .orElse(globalState(stateId, command))

  private def get(stateId: BotStateId): Option[BotState[F]] =
    states.get(stateId).map(toBotState)

  private def globalState(
      currentStateId: BotStateId,
      command: String
  ): Option[BotState[F]] =
    globalCommands.get(command) match {
      case Some(action) => get(currentStateId).map(_.copy(action = action))
      case None         => None
    }
}

object GraphBotScenario {
  case class EdgeLabel(order: Int, expectedInputPredicate: ExpectedInputPredicate)

  implicit class EdgeLabels[N](val e: DiEdge[N]) extends AnyVal {
    def by(predicate: ExpectedInputPredicate) =
      e + EdgeLabel(0, predicate)

    def byCommand(input: String) =
      e + by(ExpectedInputPredicate.TextIsEqualTo(input))
  }

  case class Node[F[_]](id: BotStateId, action: BotScript[F, Unit])

  type BotGraph[F[_]] = Graph[Node[F], LDiEdge]

  object EdgeImplicits extends LEdgeImplicits[EdgeLabel]

}
