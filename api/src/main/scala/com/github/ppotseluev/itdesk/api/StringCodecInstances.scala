package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.serialization.StringCodec

object StringCodecInstances {
  implicit val transparentStringCodec: StringCodec[String] =
    StringCodec.from(
      decoder = Right.apply,
      encoder = identity
    )

  private val chatIdBotIdRegex = "([a-zA-Z0-9]+)_(\\S+)".r

  implicit val chatIdBotKeyCodec: StringCodec[(ChatId, BotId)] =
    StringCodec.from(
      encoder = { case (chatId, botKey) =>
        s"${chatId}_$botKey"
      },
      decoder = {
        case chatIdBotIdRegex(chatId, botId) =>
          Right(chatId -> botId)
        case _ => Left(s"String should match $chatIdBotIdRegex")
      }
    )
}
