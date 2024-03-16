package com.github.ppotseluev.itdesk.bots.core.scenario

import cats.implicits._
import com.github.ppotseluev.itdesk.bots.CallContext
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
    val globalCommands: Map[String, BotScript[F, Unit]]
) {
  private object EdgeImplicits extends LEdgeImplicits[EdgeLabel[F]]
  import EdgeImplicits._

  private val states: Map[BotStateId, graph.NodeT] =
    graph.nodes
      .map(node => node.id -> node)
      .toMap

  private def extractAvailableCommands(node: graph.NodeT): List[BotCommand] =
    node.outgoing.toList.sortBy(_.order).flatMap(asCommands)

  private def toBotState(
      actionOverride: Option[BotScript[F, Unit]]
  )(node: graph.NodeT): BotState[F] =
    BotState(
      id = node.id,
      action = actionOverride.getOrElse(node.action),
      availableCommands = extractAvailableCommands(node)
    )

  private def asCommands(edge: graph.EdgeT): List[BotCommand] =
    edge.expectedInputPredicate match {
      case ExpectedInputPredicate.EqualTo(cmd) =>
        cmd :: Nil
      case ExpectedInputPredicate.AnyInput | ExpectedInputPredicate.HasPhoto =>
        Nil
      case ExpectedInputPredicate.OneOf(commands) =>
        commands
    }

  private def isMatched(ctx: CallContext)(edge: graph.EdgeT): Boolean =
    Matcher.isMatched(ctx)(edge.expectedInputPredicate)

  def transit(stateId: BotStateId, ctx: CallContext): Option[BotState[F]] =
    globalState(stateId, ctx.inputText).orElse {
      states
        .get(stateId)
        .flatMap(_.outgoing.toList.sortBy(_.order).find(isMatched(ctx)))
        .map { edge =>
          toBotState(edge.actionOverride)(edge.to)
        }
    }

  private def globalState(
      currentStateId: BotStateId,
      command: String
  ): Option[BotState[F]] =
    globalCommands.get(command) match {
      case Some(action) =>
        states
          .get(currentStateId)
          .map(toBotState(None))
          .map(_.copy(action = action))
      case None =>
        None
    }
}

object GraphBotScenario {

  /**
   * @param actionOverride overrides node action
   */
  case class EdgeLabel[F[_]](
      order: Int,
      expectedInputPredicate: ExpectedInputPredicate,
      actionOverride: Option[BotScript[F, Unit]]
  )

  implicit class DiEdgeOps[N](val e: DiEdge[N]) extends AnyVal {
    def addLabel[F[_]](
        predicate: ExpectedInputPredicate,
        order: Int = 0,
        actionOverride: Option[BotScript[F, Unit]] = None
    ) =
      e + EdgeLabel(order, predicate, actionOverride)
  }

  case class Node[F[_]](id: BotStateId, action: BotScript[F, Unit])
  object Node {
    def start[F[_]]: Node[F] = Node("start", doNothing[F])
  }

  type BotGraph[F[_]] = Graph[Node[F], LDiEdge]

}
