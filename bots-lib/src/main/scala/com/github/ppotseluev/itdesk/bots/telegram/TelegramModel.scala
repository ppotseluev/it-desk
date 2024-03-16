package com.github.ppotseluev.itdesk.bots.telegram

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.semiauto.deriveCodec

object TelegramModel {
  implicit private val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  case class TgResponse[T](
      ok: Boolean,
      result: T
  )

  object TgResponse {
    implicit def codec[T: Codec]: Codec[TgResponse[T]] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class KeyboardUpdate(
      chatId: String,
      messageId: Long,
      replyMarkup: ReplyMarkup
  )

  @ConfiguredJsonCodec
  case class KeyboardButton(text: String)

  @ConfiguredJsonCodec
  case class InlineButton(
      text: String,
      callbackData: String
  )

  @ConfiguredJsonCodec
  case class ReplyMarkup(
      keyboard: Option[Seq[Seq[KeyboardButton]]] = None,
      inlineKeyboard: Option[Seq[Seq[InlineButton]]] = None,
      isPersistent: Option[Boolean] = None,
      removeKeyboard: Option[Boolean] = None
  )

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
  }

  @ConfiguredJsonCodec
  case class FileInfo(
      fileId: String,
      fileUniqueId: String,
      fileSize: Long,
      filePath: String
  )

  @ConfiguredJsonCodec
  case class Chat(id: Long)

  @ConfiguredJsonCodec
  case class User(
      id: Long,
      firstName: String,
      lastName: Option[String],
      username: Option[String],
      isBot: Boolean
  )

  @ConfiguredJsonCodec
  case class Photo(
      fileId: String,
      fileUniqueId: String,
      fileSize: Int,
      width: Int,
      height: Int
  )

  @ConfiguredJsonCodec
  case class TgMessage(
      messageId: Long,
      from: Option[User],
      chat: Chat,
      text: Option[String],
      photo: Option[List[Photo]]
  )

  @ConfiguredJsonCodec
  case class CallbackQuery(
      id: String,
      from: User,
      message: TgMessage, //TODO check API doc, it can't be missed, right?
      data: Option[String]
  )

  @ConfiguredJsonCodec
  case class Update(
      updateId: Int,
      message: Option[TgMessage],
      callbackQuery: Option[CallbackQuery]
  )

}
