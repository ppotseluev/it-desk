package com.github.ppotseluev.itdesk.bots.core

import cats.syntax.applicative._
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

  override def apply(input: String): BotScript[F, Unit] = {
    for {
      currentStateId <- getCurrentState.map(_.getOrElse(scenario.startFrom))
      _ <- scenario.transit(currentStateId, input) match {
        case Some(newState) => process(newState)
        case None =>
          fallbackPolicy match {
            case FallbackPolicy.Ignore => ().pure[BotScript[F, *]]
          }
      }
    } yield ()
  }

  private def process(newState: BotState[F]): BotScript[F, Unit] = for {
    _ <- saveState(newState.id)
    _ <- newState.action.withAvailableCommands(newState.availableCommands)
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
