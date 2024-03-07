package com.github.ppotseluev.itdesk.bots.core

case class Message(chatId: ChatId,
                   payload: Message.Payload)

object Message {

  case class Payload(text: String, availableCommands: Seq[BotCommand])

}