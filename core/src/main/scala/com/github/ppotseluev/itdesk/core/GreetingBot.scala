package com.github.ppotseluev.itdesk.core

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.AnyInput
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.EqualTo
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.bots.telegram.HttpTelegramClient.RichResponse
import java.time.LocalDateTime
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend
import sttp.client3.UriContext
import sttp.client3.basicRequest

/**
 * Just an example of bots-lib usage
 */
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
      .getBodyOrFail()
      .map(value => (1.0 / value.toDouble).toString)
  }

  private val showTime = Node(
    "show_time",
    getTime.flatMap(t => reply(t.toString))
  )

  private val getBtcPriceNode = Node(
    "get_btc_price",
    getBtcPrice.flatMap(reply(_))
  )

  private val graph: BotGraph[F] =
    Graph(
      start ~> greet addLabel AnyInput,
      greet ~> start addLabel EqualTo("Назад"),
      greet ~> skills addLabel EqualTo("Что ты умеешь?"),
      skills ~> start addLabel EqualTo("В начало"),
      skills ~> showTime addLabel EqualTo("Покажи время!"),
      skills ~> getBtcPriceNode addLabel EqualTo("И сколько сейчас биток?"),
      showTime ~> skills addLabel EqualTo("Назад"),
      showTime ~> showTime addLabel EqualTo("Обновить"),
      getBtcPriceNode ~> skills addLabel EqualTo("Назад"),
      getBtcPriceNode ~> getBtcPriceNode addLabel EqualTo("Обновить")
    )

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map(
      "/time" -> getTime.map(_.toString).flatMap(reply[F](_)),
      "/help" -> reply("Here should be some help message")
    )
  )

  val logic = new Bot(
    scenario = scenario,
    fallbackPolicy = FallbackPolicy.Ignore
  )
}

object GreetingBot {
  def apply[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any]) = new GreetingBot[F]
}
