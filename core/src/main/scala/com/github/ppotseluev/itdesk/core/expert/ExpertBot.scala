package com.github.ppotseluev.itdesk.core.expert

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.Context
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.BotError.AccessDenied
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._

import java.time.Instant
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend

class ExpertBot[F[_]: Sync](implicit
    sttpBackend: SttpBackend[F, Any],
    expertDao: ExpertService[F]
) {
  private val getTime: BotScript[F, Instant] = execute(Sync[F].delay(Instant.now))
  private val greet: BotScript[F, Unit] =
    reply[F]("Приветствуем тебя на нашей платформе! Пожалуйста, заполни данные о себе") >>
      reply[F]("Как тебя зовут? Введи в формате Имя Фамилия")

  private val startExpertScript: BotScript[F, Unit] =
    for {
      time <- getTime
      ctx <- getContext
      isInviteValid <- hasValidInvite(ctx.user.username, time)
      _ <-
        if (isInviteValid) registerUser(ctx) >> greet
        else reply[F]("Access denied") >> raiseError[F](AccessDenied)
    } yield ()

  private def hasValidInvite(tgUsername: String, nowTime: Instant): BotScript[F, Boolean] =
    execute {
      expertDao.getInvite(tgUsername).map {
        _.exists { invite =>
          invite.validUntil.isAfter(nowTime)
        }
      }
    }

  private def registerUser(ctx: Context): BotScript[F, Unit] =
    reply("user-register-call-stub") //TODO
  private def saveName(input: String): BotScript[F, Unit] =
    reply("user-save-name-call-stub") //TODO

  private val start = Node[F]("start", startExpertScript)
  private val enterName = Node[F](
    "enter_name",
    (getInput[F] >>= saveName) >>
      reply("Спасибо, что заполнили анкету! Мы уже проверяем данные и скоро активируем ваш профиль")
  )
  private val underReview = Node[F](
    "under_review",
    reply("Пожалуйста, подождите, идет проверка данных")
  )
  private val start0 = Node[F]("start0", doNothing[F])//TODO

  private val graph: BotGraph[F] =
    Graph(
      start0 ~> start byAnyInput,
      start ~> enterName byAnyInput,
      enterName ~> underReview byAnyInput,
      underReview ~> underReview byAnyInput //todo ?
    )

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start0.id,
    globalCommands = Map(
      "/start" -> start.action
    )
  )

  val logic = new Bot(
    scenario = scenario,
    fallbackPolicy = FallbackPolicy.Ignore
  )
}

object ExpertBot {
  def apply[F[_]: Sync](implicit sttpBackend: SttpBackend[F, Any], expertDao: ExpertService[F]) =
    new ExpertBot[F]
}
