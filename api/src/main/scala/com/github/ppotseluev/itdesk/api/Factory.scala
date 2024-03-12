package com.github.ppotseluev.itdesk.api

import cats.Parallel
import cats.effect.Async
import cats.effect.IO
import cats.effect.Resource
import com.github.ppotseluev.itdesk.api.telegram.TelegramWebhook
import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.Context
import com.github.ppotseluev.itdesk.bots.core._
import com.github.ppotseluev.itdesk.bots.runtime._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient
import com.github.ppotseluev.itdesk.bots.telegram.TelegramChatService
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient
import com.github.ppotseluev.itdesk.core.InvitationsDao
import com.github.ppotseluev.itdesk.core.MysqlInvitationsDao
import com.github.ppotseluev.itdesk.core.admin.AdminBot
import com.github.ppotseluev.itdesk.core.expert.ExpertBot
import com.github.ppotseluev.itdesk.core.expert.ExpertService
import com.github.ppotseluev.itdesk.storage._
import doobie.util.transactor.Transactor
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import pureconfig.ConfigSource
import pureconfig._
import pureconfig.configurable.genericMapReader
import pureconfig.error.FailureReason
import pureconfig.error.KeyNotFound
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

import StringCodecInstances._

class Factory[F[_]: Async: Parallel] {

  private implicit def enumMapReader[V: ConfigReader]: ConfigReader[Map[BotType, V]] =
    genericMapReader { name =>
      BotType
        .withNameOption(name)
        .fold[Either[FailureReason, BotType]](Left(KeyNotFound(s"$name is not enum")))(Right(_))
    }

  val config: Config = ConfigSource
    .resources("application.local.conf")
    .optional
    .withFallback(ConfigSource.resources("application.conf"))
    .load[Config]
    .fold(
      error => throw new RuntimeException(error.prettyPrint()),
      identity
    )

  lazy val prometheusMetrics: PrometheusMetrics[F] = PrometheusMetrics.default[F]()

  implicit def telegramClient(implicit sttpBackend: SttpBackend[F, Any]): TelegramClient[F] =
    new HttpTelegramClient(config.telegramUrl)

  implicit def chatService(implicit tgClient: TelegramClient[F]): ChatService[F] =
    new TelegramChatService(
      tgClient
    )

  implicit def botInterpreter(implicit chatService: ChatService[F]) =
    new BotInterpreterImpl(
      botStateDao,
      chatService
    )(_)

  lazy val botStateDao: BotStateDao[F] = {
    implicit val botInfoCodec: Codec[BotInfo] = deriveCodec
    implicit val keySchema: Schema.String[(ChatId, BotId)] = Schema.String(implicitly)
    implicit val scenarioSchema: Schema[BotInfo] = Schema.Json(implicitly)
    new MySqlKeyValueDao(config.botStatesTable, transactor)
  }

  implicit lazy val invitationsDao: InvitationsDao[F] = new MysqlInvitationsDao[F]

  implicit lazy val expertService: ExpertService[F] = ExpertService[F]

  private implicit lazy val transactor: Transactor[F] = {
    import config.dbConfig._
    Transactor
      .fromDriverManager[F]
      .apply(
        "com.mysql.cj.jdbc.Driver",
        url,
        user,
        password,
        None
      )
  }

  def withSttp[C](
      create: SttpBackend[F, Any] => C
  ): Resource[F, C] =
    HttpClientFs2Backend
      .resource[F]()
      .map(create)

  private def botLogic(botType: BotType)(implicit sttp: SttpBackend[F, Any]): BotLogic[F] =
    botType match {
      case BotType.AdminBot  => AdminBot[F].logic
      case BotType.ExpertBot => ExpertBot[F].logic
    }

  private def bots(implicit sttp: SttpBackend[F, Any]): Map[WebhookSecret, BotBundle[F]] =
    config.bots.map { case (botType, cfg) =>
      cfg.webhookSecret -> BotBundle(
        botType = botType,
        token = cfg.token,
        webhookSecret = cfg.webhookSecret,
        logic = botLogic(botType),
        chatId = cfg.chatId
      )
    }

  def telegramWebhookHandler(implicit
      botInterpreter: Context => BotInterpreter[F],
      sttp: SttpBackend[F, Any]
  ): TelegramWebhook.Handler[F] =
    new TelegramWebhook.Handler[F](
      botInterpreter = botInterpreter,
      bots = bots
    )

  def api(implicit
      botInterpreter: Context => BotInterpreter[F],
      sttp: SttpBackend[F, Any]
  ): Api[F] = new Api(
    telegramHandler = telegramWebhookHandler,
    telegramWebhookSecrets = config.bots.values.map(_.webhookSecret).toSet,
    prometheusMetrics = prometheusMetrics,
    config = config.apiConfig
  )
}

object Factory {
  implicit val io = {
    new Factory[IO]
  }
}
