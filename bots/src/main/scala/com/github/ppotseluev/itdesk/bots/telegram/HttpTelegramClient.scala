package com.github.ppotseluev.itdesk.bots.telegram

import cats.MonadError
import io.circe.Printer
import io.circe.syntax._
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient.RichResponse
import sttp.client3.{Response, SttpBackend, UriContext, basicRequest}
import sttp.model.{Header, MediaType, StatusCode}

class HttpTelegramClient[F[_]](telegramUrl: String)(implicit
    sttpBackend: SttpBackend[F, Any],
    F: MonadError[F, Throwable]
) extends TelegramClient[F] {

  override def send(botToken: String)(messageSource: TelegramClient.MessageSource): F[Unit] = {
    val json = Printer.noSpaces
      .copy(dropNullValues = true)
      .print(messageSource.asJson)
    basicRequest
      .post(uri"$telegramUrl/bot$botToken/sendMessage")
      .header(Header.contentType(MediaType.ApplicationJson))
      .body(json)
      .send(sttpBackend)
      .checkStatusCode()
      .void
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
