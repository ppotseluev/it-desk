package com.github.ppotseluev.itdesk.bots.telegram

import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.MessageSource
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.semiauto.deriveCodec

trait TelegramClient[F[_]] {
  def send(botToken: String)(messageSource: MessageSource): F[Unit]
}

object TelegramClient {
  implicit private val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  @ConfiguredJsonCodec
  case class KeyboardButton(text: String)

  object KeyboardButton {
    implicit val keyboardButtonCodec: Codec[KeyboardButton] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class ReplyMarkup(
      keyboard: Option[Seq[Seq[KeyboardButton]]] = None,
      removeKeyboard: Option[Boolean] = None
  )

  object ReplyMarkup {
    implicit val keyboardCodec: Codec[ReplyMarkup] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class MessageSource(chatId: String, text: String, replyMarkup: Option[ReplyMarkup])

  object MessageSource {
    implicit val messageSourceCodec: Codec[MessageSource] = deriveCodec
  }

}
