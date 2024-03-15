package com.github.ppotseluev.itdesk.bots.telegram

import cats.MonadThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotCommand
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.bots.core.Message
import com.github.ppotseluev.itdesk.bots.runtime.ChatService
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.InlineButton
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.KeyboardButton
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.ReplyMarkup

class TelegramChatService[F[_]: MonadThrow](telegramClient: TelegramClient[F])
    extends ChatService[F] {

  override def send(botToken: String)(chatId: ChatId)(message: Message): F[Unit] =
    buildKeyboard(message.availableCommands).flatMap { keyboard =>
      val msg = TelegramClient.MessageSource(chatId, message.payload.text, Some(keyboard))
      telegramClient.send(botToken)(msg, message.payload.photo)
    }

  private def buildKeyboard(availableCommands: Seq[BotCommand]): F[ReplyMarkup] = {
    if (availableCommands.isEmpty)
      ReplyMarkup(removeKeyboard = Some(true)).pure[F]
    else {
      val regularCommands = availableCommands.collect { case BotCommand.Regular(text) =>
        text
      }
      val callbackCommands = availableCommands.collect { case c: BotCommand.Callback =>
        c
      }
      if (regularCommands.nonEmpty && callbackCommands.nonEmpty) {
        new IllegalArgumentException(
          "Bot commands can't contain both regular and callback commands"
        ).raiseError[F, ReplyMarkup]
      } else if (regularCommands.nonEmpty) {
        val buttons = regularCommands
          .map(KeyboardButton.apply)
          .map(Seq(_))
        ReplyMarkup(keyboard = Some(buttons)).pure[F]
      } else {
        val buttons = callbackCommands
          .map(c => InlineButton(c.text, c.callbackData))
          .map(Seq(_))
        ReplyMarkup(inlineKeyboard = Some(buttons)).pure[F]
      }
    }
  }
}
