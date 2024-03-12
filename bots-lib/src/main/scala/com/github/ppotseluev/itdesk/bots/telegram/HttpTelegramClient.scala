package com.github.ppotseluev.itdesk.bots.telegram

import cats.MonadError
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient.RichResponse
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.MessageSource
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient.MessageSource.PhotoUrl
import io.circe.Json
import io.circe.Printer
import io.circe.syntax._
import sttp.client3.Response
import sttp.client3.SttpBackend
import sttp.client3.UriContext
import sttp.client3._
import sttp.client3.basicRequest
import sttp.client3.circe._
import sttp.model.Header
import sttp.model.MediaType
import sttp.model.StatusCode

class HttpTelegramClient[F[_]](telegramUrl: String)(implicit
    sttpBackend: SttpBackend[F, Any],
    F: MonadError[F, Throwable]
) extends TelegramClient[F] {

  override def send(
      botToken: String
  )(messageSource: MessageSource, photo: Option[Either[PhotoUrl, Array[Byte]]]): F[Unit] = {
    val json = Printer.noSpaces
      .copy(dropNullValues = true)
      .print(messageSource.asJson)
    val sendText = basicRequest
      .post(uri"$telegramUrl/bot$botToken/sendMessage")
      .header(Header.contentType(MediaType.ApplicationJson))
      .body(json)
      .send(sttpBackend)
      .checkStatusCode()
      .void
    photo match {
      case Some(value) =>
        if (messageSource.text.length > 1024) {
          sendImage(botToken, messageSource.copy(text = ""), value) *> sendText
        } else {
          sendImage(botToken, messageSource, value)
        }
      case None => sendText
    }
  }

  private def sendImage(
      botToken: String,
      source: MessageSource,
      photo: Either[PhotoUrl, Array[Byte]]
  ): F[Unit] = {
    val request = basicRequest.post(uri"$telegramUrl/bot$botToken/sendPhoto")
    val photoRequest = photo match {
      case Right(bytes) =>
        request.multipartBody(
          multipart("photo", bytes).fileName("image"),
          multipart("chat_id", source.chatId),
          multipart("caption", source.text)
        )
      case Left(url) =>
        val json = Json.obj(
          "photo" -> url.asJson,
          "chat_id" -> source.chatId.asJson,
          "caption" -> source.text.asJson
        )
        request.header(Header.contentType(MediaType.ApplicationJson)).body(json)
    }
    photoRequest.send(sttpBackend).checkStatusCode().void
  }
}

object HttpTelegramClient {
  case class HttpCodeException(code: Int, message: String)
      extends RuntimeException(s"Bad status code: $code, $message")

  implicit class RichResponse[F[_], T](val responseF: F[Response[T]]) extends AnyVal {

    def checkStatusCode(
        isSuccess: StatusCode => Boolean = _.isSuccess
    )(implicit F: MonadError[F, Throwable], ev: T <:< Either[String, _]): F[Response[T]] =
      responseF.flatMap { response =>
        if (isSuccess(response.code))
          response.pure[F]
        else
          HttpCodeException(response.code.code, response.body.left.getOrElse(""))
            .raiseError[F, Response[T]]
      }
  }
}
