package com.github.ppotseluev.itdesk.bots.core

import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario

/**
 * Describes bot logic using [[BotDsl]]
 */
class Bot[F[_]](
    scenario: GraphBotScenario[F],
    fallbackPolicy: FallbackPolicy
) extends BotLogic[F] {

  override def apply(ctx: CallContext): BotScript[F, Unit] = {
    for {
      currentStateId <- getCurrentState.map(_.getOrElse(scenario.startFrom))
      _ <- scenario.transit(currentStateId, ctx) match {
        case Some(newState) => process(currentStateId, newState)
        case None =>
          fallbackPolicy match {
            case FallbackPolicy.Ignore => doNothing[F]
          }
      }
    } yield ()
  }

  private def process(currentStateId: BotStateId, newState: BotState[F]): BotScript[F, Unit] = for {
    _ <- newState.action.withAvailableCommands(newState.availableCommands)
    _ <- if (newState.id != currentStateId) saveState[F](newState.id) else doNothing[F]
  } yield ()
}

object Bot {

  /**
   * Defines wow to handle unexpected commands
   */
  sealed trait FallbackPolicy

  object FallbackPolicy {

    case object Ignore extends FallbackPolicy

  }

}
