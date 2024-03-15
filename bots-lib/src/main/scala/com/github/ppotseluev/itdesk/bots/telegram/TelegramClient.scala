package com.github.ppotseluev.itdesk.bots.telegram

import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.FileInfo
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.MessageSource
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.MessageSource.PhotoUrl
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.semiauto.deriveCodec

trait TelegramClient[F[_]] {
  def send(botToken: String)(
      messageSource: MessageSource,
      photo: Option[Either[PhotoUrl, Array[Byte]]]
  ): F[Unit]

  def getFile(botToken: String, fileId: String): F[FileInfo]

  def downloadFile(botToken: String, filePath: String): F[Array[Byte]]
}

object TelegramClient {
  case class TgResponse[T](
      ok: Boolean,
      result: T
  )
  object TgResponse {
    implicit def codec[T: Codec]: Codec[TgResponse[T]] = deriveCodec
  }

  implicit private val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  @ConfiguredJsonCodec
  case class KeyboardButton(text: String)

  object KeyboardButton {
    implicit val keyboardButtonCodec: Codec[KeyboardButton] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class ReplyMarkup(
      keyboard: Option[Seq[Seq[KeyboardButton]]] = None,
      isPersistent: Boolean = true,
      removeKeyboard: Option[Boolean] = None
  )

  object ReplyMarkup {
    implicit val keyboardCodec: Codec[ReplyMarkup] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class MessageSource(
      chatId: String,
      text: String,
      replyMarkup: Option[ReplyMarkup],
      parseMode: Option[String] = None, //Some("MarkdownV2")
      disableWebPagePreview: Option[Boolean] = None
  )

  object MessageSource {

    /**
     * Url or tg file_id
     */
    type PhotoUrl = String
    implicit val messageSourceCodec: Codec[MessageSource] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class FileInfo(
      fileId: String,
      fileUniqueId: String,
      fileSize: Long,
      filePath: String
  )

}
