package com.github.ppotseluev.itdesk.bots.core

import cats.free.Free
import cats.free.Free.liftF

sealed trait BotDsl[T]

object BotDsl {
  /**
   * Free monad based bot's EDSL
   */
  type BotScript[T] = Free[BotDsl, T]

  case class GetCurrentState(chatId: ChatId, botId: BotId) extends BotDsl[Option[BotStateId]]

  case class SaveState(chatId: ChatId, botId: BotId, botStateId: BotStateId) extends BotDsl[Unit]

  case class Reply(botId: BotId, chatId: ChatId, message: Message.Payload) extends BotDsl[Unit]

  def getCurrentState(chatId: ChatId, botId: BotId): BotScript[Option[BotStateId]] =
    liftF(GetCurrentState(chatId, botId))

  def saveState(chatId: ChatId, botId: BotId, botStateId: BotStateId): BotScript[Unit] =
    liftF(SaveState(chatId, botId, botStateId))

  def reply(botId: BotId, chatId: ChatId, message: Message.Payload): BotScript[Unit] =
    liftF(Reply(botId, chatId, message))
}

