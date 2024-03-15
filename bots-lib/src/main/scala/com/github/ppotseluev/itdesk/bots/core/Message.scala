package com.github.ppotseluev.itdesk.bots.core

import com.github.ppotseluev.itdesk.bots.telegram.TelegramModel.MessageSource.PhotoUrl

case class Message(
    payload: Message.Payload,
    availableCommands: Seq[BotCommand]
)

object Message {

  case class Payload(
      text: String,
      photo: Option[Either[PhotoUrl, Array[Byte]]]
  )

}
