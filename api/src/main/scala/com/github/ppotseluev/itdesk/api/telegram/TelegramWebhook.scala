package com.github.ppotseluev.itdesk.api.telegram

import cats.Monad
import cats.implicits._
import cats.effect.kernel.Async
import com.github.ppotseluev.itdesk.api.BotBundle
import com.github.ppotseluev.itdesk.bots.core.{BotId, BotInput, BotLogic, Message}
import com.github.ppotseluev.itdesk.bots.runtime.BotInterpreter
import io.circe.Codec
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.{Endpoint, auth, endpoint, header, stringBody}

object TelegramWebhook {
  implicit private val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  @ConfiguredJsonCodec
  case class Chat(id: Int)

  object Chat {
    implicit val codec: Codec[Chat] = deriveCodec
  }

  @ConfiguredJsonCodec
  case class User(id: UserId, firstName: String, lastName: Option[String], username: Option[String])

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

  class Handler[F[_]: Monad](
      allowedUsers: Set[UserId],
      trackedChats: Set[String],
      botInterpreter: BotInterpreter[F],
      bots: Map[WebhookSecret, BotBundle]
  ) {
    private val success = ().asRight[Error].pure[F]

    private def skip = success

    def handleTelegramEvent(webhookSecret: WebhookSecret)(update: Update): F[Either[Error, Unit]] =
      update.message match {
        case Some(TgMessage(_, Some(user), chat, Some(text))) =>
          val chatId = chat.id.toString
          val shouldReact =
            allowedUsers.contains(user.id) &&
              trackedChats.contains(chatId)
          if (shouldReact) {
            val message = Message(
              payload = Message.Payload(text, Seq.empty),
              chatId = chat.id.toString
            )
            val bot = bots(webhookSecret)
            val botInput = BotInput(bot.botType.id, message) //todo bot get is unsafe
            bot.logic(botInput).foldMap(botInterpreter).map(_.asRight)
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