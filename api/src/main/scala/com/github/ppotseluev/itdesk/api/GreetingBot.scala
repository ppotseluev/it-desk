package com.github.ppotseluev.itdesk.api

import cats.effect.{Ref, Sync}
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.AnyInput
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient.RichResponse

import java.time.LocalDateTime
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend
import sttp.client3.UriContext
import sttp.client3.basicRequest

class GreetingBot[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any]) {

  private def start = Node[F]("start", reply("Как тебя зовут?"))
  private def greet = Node[F]("greet", getInput.flatMap(name => reply(s"Привет $name")))
  private def skills = Node[F](
    "skills",
    reply("Я умею показывать время и цену BTC :)")
  )
  private val getTime = execute {
    Sync[F].delay {
      LocalDateTime.now()
    }
  }

  private val getBtcPrice = execute {
    basicRequest
      .get(uri"https://blockchain.info/tobtc?currency=USD&value=1")
      .send(sttpBackend)
      .checkStatusCode()
      .map(_.body)
      .map {
        case Left(value)  => value
        case Right(value) => (1.0 / value.toDouble).toString
      }
  }

  private val showTime = Node(
    "show_time",
    getTime.flatMap(t => reply(t.toString))
  )

  private val getBtcPriceNode = Node(
    "get_btc_price",
    getBtcPrice.flatMap(reply)
  )

  private val graph: BotGraph[F] =
    Graph(
      start ~> greet by AnyInput,
      greet ~> start byCommand "Назад",
      greet ~> skills byCommand "Что ты умеешь?",
      skills ~> greet byCommand "Назад",
      skills ~> start byCommand "В начало",
      skills ~> showTime byCommand "Покажи время!",
      skills ~> getBtcPriceNode byCommand "И сколько сейчас биток?",
      showTime ~> skills byCommand "Назад",
      showTime ~> showTime byCommand "Обновить",
      getBtcPriceNode ~> skills byCommand "Назад",
      getBtcPriceNode ~> getBtcPriceNode byCommand "Обновить"
    )

  val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
      "/time" -> getTime.map(_.toString).flatMap(reply[F]),
      "/help" -> reply("Here should be some help message")
    )
  )
}

object GreetingBot {
  def apply[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any]) = new GreetingBot[F]
}
