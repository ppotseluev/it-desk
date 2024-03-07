package com.github.ppotseluev.itdesk.api

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  val factory = Factory.io

  override def run(args: List[String]): IO[ExitCode] = {
    import factory._
    factory.telegramClient.use { implicit tg =>
      factory.api.runServer
    }
  }
}
