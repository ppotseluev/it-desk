package com.github.ppotseluev.itdesk.bots.telegram

import com.github.ppotseluev.itdesk.bots.core.BotCommand
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.bots.core.Message
import com.github.ppotseluev.itdesk.bots.runtime.ChatService
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.KeyboardButton
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.ReplyMarkup

class TelegramChatService[F[_]](telegramClient: TelegramClient[F]) extends ChatService[F] {

  override def send(botToken: String)(chatId: ChatId)(payload: Message.Payload): F[Unit] = {
    val keyboard = buildKeyboard(payload.availableCommands)
    val message = TelegramClient.MessageSource(chatId, payload.text, Some(keyboard))
    telegramClient.send(botToken)(message)
  }

  private def buildKeyboard(availableCommands: Seq[BotCommand]): ReplyMarkup =
    if (availableCommands.isEmpty)
      ReplyMarkup(removeKeyboard = Some(true))
    else {
      val buttons = availableCommands
        .map(KeyboardButton.apply)
        .map(Seq(_))
      ReplyMarkup(keyboard = Some(buttons))
    }
}
