package com.github.ppotseluev.itdesk.bots.telegram

import com.github.ppotseluev.itdesk.bots.telegram.TelegramModel.MessageSource.PhotoUrl

import TelegramModel._

trait TelegramClient[F[_]] {
  def send(botToken: String)(
      messageSource: MessageSource,
      photo: Option[Either[PhotoUrl, Array[Byte]]]
  ): F[Unit]

  def getFile(botToken: String, fileId: String): F[FileInfo]

  def downloadFile(botToken: String, filePath: String): F[Array[Byte]]

  def editInlineKeyboard(
      botToken: String,
      keyboardUpdate: KeyboardUpdate
  ): F[Unit]
}
