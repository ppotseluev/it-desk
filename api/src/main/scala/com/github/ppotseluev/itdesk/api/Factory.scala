package com.github.ppotseluev.itdesk.api

import cats.{ApplicativeError, Parallel}
import cats.effect.{Async, Concurrent, IO, Ref, Resource}
import cats.effect.std.Console
import cats.implicits._
import StringCodecInstances._
import com.github.ppotseluev.itdesk.api.telegram.{TelegramWebhook, WebhookSecret}
import com.github.ppotseluev.itdesk.storage._
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.runtime._
import com.github.ppotseluev.itdesk.bots.core._
import com.github.ppotseluev.itdesk.bots.telegram.{
  HttpTelegramClient,
  TelegramChatService,
  TelegramClient
}
import com.github.ppotseluev.itdesk.storage.MySqlConfig
import doobie.util.transactor.Transactor
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.enumeratum._
import pureconfig.syntax._
import pureconfig._
import pureconfig.configurable.genericMapReader
import pureconfig.error.{FailureReason, KeyNotFound}
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

import java.io.File
import java.time.ZoneOffset

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

  lazy val telegramClient: Resource[F, TelegramClient[F]] =
    createHttpClient[TelegramClient[F]] { implicit sttp =>
      new HttpTelegramClient(config.telegramUrl)
    }

  implicit def chatService(implicit tgClient: TelegramClient[F]): ChatService[F] =
    new TelegramChatService(
      tgClient
    )

  implicit def botInterpreter(implicit chatService: ChatService[F]): BotInterpreter[F] =
    new BotInterpreterImpl(
      botStateDao,
      chatService,
      id => config.botWithId(id).token
    )

  lazy val botStateDao: BotStateDao[F] = {
    implicit val botInfoCodec: Codec[BotInfo] = deriveCodec
    implicit val keySchema: Schema.String[(ChatId, BotId)] = Schema.String(implicitly)
    implicit val scenarioSchema: Schema[BotInfo] = Schema.Json(implicitly)
    new MySqlKeyValueDao(config.botStatesTable, transactor(config.dbConfig))
  }

  private def transactor(mySqlConfig: MySqlConfig) =
    Transactor
      .fromDriverManager[F]
      .apply(
        "com.mysql.cj.jdbc.Driver",
        mySqlConfig.url,
        mySqlConfig.user,
        mySqlConfig.password,
        None
      )

  private def createHttpClient[C](
      create: SttpBackend[F, Any] => C
  ): Resource[F, C] =
    HttpClientFs2Backend
      .resource[F]()
      .map(create)

  private def botLogic(botType: BotType): BotLogic = botType match {
    case BotType.GreetingBot =>
      new Bot(
        scenario = GreetingBot.scenario,
        fallbackPolicy = FallbackPolicy.Ignore
      )
  }

  private val bots: Map[WebhookSecret, BotBundle] = config.bots.map { case (botType, cfg) =>
    cfg.webhookSecret -> BotBundle(
      botType = botType,
      token = cfg.token,
      webhookSecret = cfg.webhookSecret,
      logic = botLogic(botType)
    )
  }

  def telegramWebhookHandler(implicit
      botInterpreter: BotInterpreter[F]
  ): TelegramWebhook.Handler[F] =
    new TelegramWebhook.Handler[F](
      allowedUsers = config.telegramUsersWhitelist,
      trackedChats = config.telegramTrackedChats,
      botInterpreter = botInterpreter,
      bots = bots
    )

  def api(implicit
      botInterpreter: BotInterpreter[F]
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
