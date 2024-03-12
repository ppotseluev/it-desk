package com.github.ppotseluev.itdesk.api.telegram

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits._
import com.github.ppotseluev.itdesk.api.BotBundle
import com.github.ppotseluev.itdesk.bots.Context
import com.github.ppotseluev.itdesk.bots.TgUser
import com.github.ppotseluev.itdesk.bots.core.BotError
import com.github.ppotseluev.itdesk.bots.runtime.BotInterpreter
import com.typesafe.scalalogging.LazyLogging
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.semiauto.deriveCodec
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

  @ConfiguredJsonCodec
  case class Chat(id: Long)

  object Chat {
    implicit val codec: Codec[Chat] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class User(
      id: UserId,
      firstName: String,
      lastName: Option[String],
      username: Option[String],
      isBot: Boolean
  )

  object User {
    implicit val codec: Codec[User] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class TgMessage(messageId: Int, from: Option[User], chat: Chat, text: Option[String])

  object TgMessage {
    implicit val codec: Codec[TgMessage] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class Update(updateId: Int, message: Option[TgMessage])

  object Update {
    implicit val codec: Codec[Update] = deriveCodec
  }

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
      botInterpreter: Context => BotInterpreter[F],
      bots: Map[WebhookSecret, BotBundle[F]]
  ) {
    private val success = ().asRight[Error].pure[F]

    private def skip = success

    def handleTelegramEvent(webhookSecret: WebhookSecret)(update: Update): F[Either[Error, Unit]] =
      update.message match {
        case Some(TgMessage(_, Some(user), chat, Some(rawInput))) if !user.isBot =>
          val input = rawInput.stripSuffix("@it_desk_admin_bot") //TODO it's a workaround
          val chatId = chat.id.toString
          val bot = bots(webhookSecret)
          val shouldReact = bot.chatId.forall(_ == chatId)
          if (shouldReact) {
            val ctx = Context(
              botToken = bot.token,
              botId = bot.botType.id,
              chatId = chatId,
              input = input,
              user = TgUser(
                id = user.id,
                username = user.username.getOrElse("UNDEFINED_USERNAME")
              )
            )
            val f = bot.logic(input).foldMap(botInterpreter(ctx))
            f.recoverWith { case e: BotError =>
              Sync[F].delay(logger.warn("Bot execution exception", e))
            }.map(_.asRight)
          } else {
            skip
          }
        case _ => skip
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
