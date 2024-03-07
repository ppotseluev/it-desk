package com.github.ppotseluev.itdesk.bots.core

import cats.syntax.applicative._
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario

/**
 * Describes bot logic using [[BotDsl]]
 */
class Bot(
    scenario: GraphBotScenario,
    fallbackPolicy: FallbackPolicy
) extends BotLogic {

  override def apply(botInput: BotInput): BotScript[Unit] = {
    val BotInput(botId, Message(chatId, payload)) = botInput
    for {
      currentStateId <- getCurrentState(chatId, botId).map(_.getOrElse(scenario.startFrom))
      _ <- scenario.transit(currentStateId, payload) match {
        case Some(newState) => process(botInput, newState)
        case None =>
          fallbackPolicy match {
            case FallbackPolicy.Ignore => ().pure[BotScript]
          }
      }
    } yield ()
  }

  private def process(botInput: BotInput, newState: BotState): BotScript[Unit] = for {
    _ <- saveState(botInput.message.chatId, botInput.botId, newState.id)
    _ <- newState.action match {
      case Action.Reply(text) =>
        val payload = Message.Payload(text, newState.availableCommands)
        reply(botInput.botId, botInput.message.chatId, payload)
    }
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
