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
//todo remove botId chatid from Input. Do we need payload here?
  override def apply(payload: Message.Payload): BotScript[F, Unit] = {
    for {
      currentStateId <- getCurrentState.map(_.getOrElse(scenario.startFrom))
      _ <- scenario.transit(currentStateId, payload) match {
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
    _ <- newState.action.withAvailableCommands(newState.availableCommands) //TODO is it a right place to do it?
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
