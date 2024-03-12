package com.github.ppotseluev.itdesk.bots.core.scenario

import cats.implicits._
import com.github.ppotseluev.itdesk.bots.Context
import com.github.ppotseluev.itdesk.bots.core.BotDsl.BotScript
import com.github.ppotseluev.itdesk.bots.core.BotDsl.doNothing
import com.github.ppotseluev.itdesk.bots.core._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
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
      case ExpectedInputPredicate.AnyInput | ExpectedInputPredicate.HasPhoto =>
        None
    }

  private def isMatched(ctx: Context)(edge: graph.EdgeT): Boolean =
    Matcher.isMatched(ctx)(edge.expectedInputPredicate)

  def transit(stateId: BotStateId, ctx: Context): Option[BotState[F]] =
    globalState(stateId, ctx.inputText).orElse {
      states
        .get(stateId)
        .flatMap(_.outgoing.toList.sortBy(_.order).find(isMatched(ctx)))
        .map(_.to)
        .map(toBotState)
    }

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

  object EdgeLabel {
    def command(command: String, order: Int): EdgeLabel =
      EdgeLabel(order, ExpectedInputPredicate.TextIsEqualTo(command))
  }

  implicit class LDiEdgeAssoc[N](val e: DiEdge[N]) extends AnyVal {
    def by(input: String, order: Int = 0) =
      e + EdgeLabel.command(input, order)
    def byAnyInput(order: Int) =
      e + EdgeLabel(order, ExpectedInputPredicate.AnyInput)
    def byAnyInput =
      byAnyInput(0)
    def byAnyPhoto(order: Int) =
      e + EdgeLabel(order, ExpectedInputPredicate.HasPhoto)
    def byAnyPhoto = byAnyPhoto(0)
  }

  case class Node[F[_]](id: BotStateId, action: BotScript[F, Unit])
  object Node {
    def start[F[_]]: Node[F] = Node("start", doNothing[F])
  }

  type BotGraph[F[_]] = Graph[Node[F], LDiEdge]

  object EdgeImplicits extends LEdgeImplicits[EdgeLabel]

}
