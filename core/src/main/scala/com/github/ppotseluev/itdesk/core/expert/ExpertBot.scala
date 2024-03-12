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
    expertService: ExpertService[F]
) {
  private val getTime: BotScript[F, Instant] = execute(Sync[F].delay(Instant.now))
  private val greet: BotScript[F, Unit] =
    reply[F]("Приветствуем тебя на нашей платформе! Пожалуйста, заполни данные о себе") >>
      reply[F]("Как тебя зовут? Введи в формате Имя Фамилия")

  private val checkExpertScript: BotScript[F, Unit] =
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
      expertService.getInvite(tgUsername).map {
        _.exists { invite =>
          invite.validUntil.isAfter(nowTime)
        }
      }
    }

  private def registerUser(ctx: Context): BotScript[F, Unit] = execute {
    expertService.register(ctx.user.id)
  }

  private def updateInfo(f: (Context, Expert.Info) => Expert.Info): BotScript[F, Unit] =
    getContext[F].flatMap { ctx =>
      execute {
        expertService.updateInfo(
          tgUserId = ctx.user.id,
          info = f(ctx, Expert.Info.empty)
        )
      }
    }

  private def name(ctx: Context, info: Expert.Info): Expert.Info =
    info.copy(name = ctx.inputText.some)

  private def description(ctx: Context, info: Expert.Info): Expert.Info =
    info.copy(description = ctx.inputText.some)

  private def photo(ctx: Context, info: Expert.Info): Expert.Info = {
    val photo = ctx.inputPhoto.flatMap(_.maxByOption(_.width))
    info.copy(photo = photo.map(_.fileId))
  }

  private val start = Node.start[F]
  private val verifyAndAskName = Node[F]("check", checkExpertScript)
  private val nameAdded = Node[F](
    "enter_name",
    updateInfo(name) >>
      reply("Ок. Теперь расскажите, пожалуйста, о себе. Это описание будет видно студентам")
  )
  private val descriptionAdded = Node[F](
    "enter_description",
    updateInfo(description) >>
      reply("Теперь загрузите фото")
  )
  private val photoAdded = Node[F](
    "add_photo",
    updateInfo(photo) >>
      reply("Спасибо, что заполнили анкету! Мы уже проверяем данные и скоро активируем ваш профиль")
  )
  private val underReview = Node[F](
    "under_review",
    reply("Пожалуйста, подождите, идет проверка данных")
  )

  private val graph: BotGraph[F] =
    Graph(
      start ~> verifyAndAskName by "/start",
      verifyAndAskName ~> nameAdded byAnyInput,
      nameAdded ~> descriptionAdded byAnyInput,
      descriptionAdded ~> photoAdded byAnyPhoto 0,
      descriptionAdded ~> descriptionAdded byAnyInput 1,
      photoAdded ~> underReview byAnyInput,
      underReview ~> underReview byAnyInput
    )

  private val scenario: GraphBotScenario[F] = new GraphBotScenario(
    graph = graph,
    startFrom = start.id,
    globalCommands = Map.empty
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
