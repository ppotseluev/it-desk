package com.github.ppotseluev.itdesk.api

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient.RichResponse
import io.circe.syntax._
import sttp.client3.Response
import sttp.client3.SttpBackend
import sttp.client3.UriContext
import sttp.client3.basicRequest

import java.time.LocalDateTime
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.basicRequest
import sttp.model.{Header, MediaType}

class GreetingBot[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any]) {

  private def start = Node[F]("start", reply("Привет"))
  private def about = Node[F]("about", reply("Я телеграм-бот!"))
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
      start ~> about by "Show info",
      about ~> start by "Назад",
      about ~> skills by "Что ты умеешь?",
      skills ~> about by "Назад",
      skills ~> start by "В начало",
      skills ~> showTime by "Покажи время!",
      skills ~> getBtcPriceNode by "И сколько сейчас биток?",
      showTime ~> skills by "Назад",
      showTime ~> showTime by "Обновить",
      getBtcPriceNode ~> skills by "Назад",
      getBtcPriceNode ~> getBtcPriceNode by "Обновить"
    )

  val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
//      "/start" -> goTo(about2.id),
      "/time" -> getTime.map(_.toString).flatMap(reply[F]),
      "/help" -> reply("Here should be some help message")
    )
  )
}

object GreetingBot {
  def apply[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any]) = new GreetingBot[F]
}
