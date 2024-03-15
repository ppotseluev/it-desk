package com.github.ppotseluev.itdesk.api.telegram

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits._
import com.github.ppotseluev.itdesk.api.BotBundle
import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.BotError
import com.github.ppotseluev.itdesk.bots.runtime.BotInterpreter
import com.github.ppotseluev.itdesk.bots.telegram.TelegramModel._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.extras.Configuration
import sttp.tapir.Endpoint
import sttp.tapir._
import sttp.tapir.auth
import sttp.tapir.endpoint
import sttp.tapir.generic.auto._
import sttp.tapir.header
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.stringBody

object TelegramWebhook extends LazyLogging {
  implicit private val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  private val baseEndpoint = endpoint

  type Error = String

  val webhookEndpointDef: Endpoint[WebhookSecret, Update, Error, Unit, Any] =
    baseEndpoint
      .in("telegram")
      .post
      .in(jsonBody[Update])
      .errorOut(stringBody)
      .securityIn(auth.apiKey(header[WebhookSecret]("X-Telegram-Bot-Api-Secret-Token")))

  class Handler[F[_]: Sync](
      botInterpreter: CallContext => BotInterpreter[F],
      bots: Map[WebhookSecret, BotBundle[F]]
  ) {
    private val success = ().asRight[Error].pure[F]

    private def skip = success

    def handleTelegramEvent(
        webhookSecret: WebhookSecret
    )(update: Update): F[Either[Error, Unit]] = Sync[F].delay {
      val bot = bots(webhookSecret)
      logger.info(s"[${bot.botType}] received $update")
    } >> {
      val input = update.message
        .flatMap(_.text)
        .map(_.stripSuffix("@it_desk_admin_bot"))
        .orElse(update.callbackQuery.map(_.message).flatMap(_.text))
        .getOrElse("")
      val chatId = update.message
        .map(_.chat.id)
        .orElse(update.callbackQuery.map(_.message.chat.id))
        .getOrElse(???) //TODO
        .toString
      val user = update.message
        .flatMap(_.from)
        .orElse(update.callbackQuery.map(_.from))
        .getOrElse(???) //TODO
      val bot = bots(webhookSecret)
      val shouldReact = bot.chatId.forall(_ == chatId) && !user.isBot
      if (shouldReact) {
        val ctx = CallContext(
          botToken = bot.token,
          botId = bot.botType.id,
          chatId = chatId,
          inputText = input,
          user = user,
          inputPhoto = update.message.flatMap(_.photo),
          callbackQuery = update.callbackQuery
        )
        val f = bot.logic(ctx).foldMap(botInterpreter(ctx))
        f.recoverWith { case e: BotError =>
          Sync[F].delay(logger.warn("Bot execution exception", e))
        }.map(_.asRight)
      } else {
        skip
      }
    }

  }

  def webhookEndpoint[F[_]: Async](
      handler: Handler[F],
      allowedWebhookSecrets: Set[WebhookSecret]
  ) =
    webhookEndpointDef
      .serverSecurityLogicPure { secret =>
        if (allowedWebhookSecrets.contains(secret)) secret.asRight
        else "Error".asLeft
      }
      .serverLogic(handler.handleTelegramEvent)
}
