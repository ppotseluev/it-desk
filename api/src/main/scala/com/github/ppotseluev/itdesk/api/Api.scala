package com.github.ppotseluev.itdesk.api

import cats.Parallel
import cats.effect.Async
import cats.effect.ExitCode
import cats.implicits._
import com.github.ppotseluev.itdesk.api.telegram.TelegramWebhook
import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

class Api[F[_]: Async: Parallel](
    telegramHandler: TelegramWebhook.Handler[F],
    telegramWebhookSecrets: Set[WebhookSecret],
    prometheusMetrics: PrometheusMetrics[F],
    config: Api.Config
) {

  private val serverOptions = Http4sServerOptions.customiseInterceptors
    .metricsInterceptor(prometheusMetrics.metricsInterceptor())
    .options

  private def buildRoutes(endpoints: ServerEndpoint[Any, F]*): HttpRoutes[F] =
    Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)

  private val routes: HttpRoutes[F] = buildRoutes(
    TelegramWebhook.webhookEndpoint(telegramHandler, telegramWebhookSecrets)
  )

  private def run(port: Int, routes: HttpRoutes[F]): F[ExitCode] =
    BlazeServerBuilder
      .apply[F]
      .bindHttp(port = port, host = "0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Error)

  val runServer: F[ExitCode] = run(config.port, routes)
}

object Api {
  case class Config(port: Int, operationalPort: Int)
}
