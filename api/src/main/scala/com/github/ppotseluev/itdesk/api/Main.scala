package com.github.ppotseluev.itdesk.api

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp

object Main extends IOApp {

  val factory = Factory.io

  override def run(args: List[String]): IO[ExitCode] = {
    import factory._
    factory.withSttp { implicit sttp =>
      factory.api.run
    }.useEval
  }
}
