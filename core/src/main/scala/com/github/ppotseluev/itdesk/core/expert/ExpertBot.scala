package com.github.ppotseluev.itdesk.core.expert

import cats.effect.Ref
import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotCommand
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.BotError.AccessDenied
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.AnyInput
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.EqualTo
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.HasPhoto
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.OneOf
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient
import java.time.Instant
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.immutable.Graph
import sttp.client3.SttpBackend

class ExpertBot[F[_]: Sync](implicit
    sttpBackend: SttpBackend[F, Any],
    expertService: ExpertService[F],
    tg: TelegramClient[F]
) {
  private val getTime: BotScript[F, Instant] = execute(Sync[F].delay(Instant.now))
  private val greet: BotScript[F, Unit] =
    reply[F]("Приветствуем тебя на нашей платформе! Пожалуйста, заполни данные о себе") >>
      reply[F]("Как тебя зовут? Введи в формате Имя Фамилия")

  private val checkExpertScript: BotScript[F, Unit] =
    for {
      time <- getTime
      ctx <- getCallContext
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

  private def registerUser(ctx: CallContext): BotScript[F, Unit] = execute {
    expertService.register(ctx.user.id)
  }

  private def updateInfo(f: (CallContext, Expert.Info) => Expert.Info): BotScript[F, Unit] =
    getCallContext[F].flatMap { ctx =>
      execute {
        expertService.updateInfo(
          tgUserId = ctx.user.id,
          info = f(ctx, Expert.Info.empty)
        )
      }
    }

  private def name(ctx: CallContext, info: Expert.Info): Expert.Info =
    info.copy(name = ctx.inputText.some)

  private def description(ctx: CallContext, info: Expert.Info): Expert.Info =
    info.copy(description = ctx.inputText.some)

  private def photo(
      photo: Option[Array[Byte]]
  )(ctx: CallContext, info: Expert.Info): Expert.Info = {
    info.copy(photo = photo)
  }

  private def getPhoto(ctx: CallContext): BotScript[F, Option[Array[Byte]]] = execute {
    ctx.inputPhoto.flatMap(_.maxByOption(_.width)) match {
      case Some(photo) =>
        for {
          fileInfo <- tg.getFile(ctx.botToken, photo.fileId)
          file <- tg.downloadFile(ctx.botToken, fileInfo.filePath)
        } yield file.some
      case None => none[Array[Byte]].pure[F]
    }
  }

  private val start = Node.start[F]
  private val verifyAndAskName = Node[F]("check", checkExpertScript)
  private val nameAdded = Node[F](
    "enter_name",
    updateInfo(name) >>
      reply("Ок. Теперь расскажи, пожалуйста, о себе. Это описание будет видно студентам")
  )
  private val descriptionAdded = Node[F](
    "enter_description",
    updateInfo(description) >>
      reply("Теперь загрузите фото")
  )
  private val photoAdded = Node[F](
    "add_photo",
    (getCallContext[F] >>= getPhoto).flatMap { file =>
      updateInfo(photo(file))
    } >> reply(
//      "Благодарим за заполнение анкеты! Мы уже проверяем данные и скоро активируем твой профиль"
      "Теперь выберите ваши скиллы. Отметьте все подходящие"
    )
  )

  private val expertsSkills: Map[Long, Ref[F, Set[Skill]]] = Map.empty

  private val getExpert: BotScript[F, Expert] = ???
  private val addSkillScript: BotScript[F, Unit] = ???

  private val addSkill = Node[F](
    "add_skill",
    for {
      input <- getInput[F]
      expert <- getExpert
      _ <- input match {
        case s if s.startsWith("\uD83D\uDD32") => addSkillScript // add skill (select)
        case s if s.startsWith("✅")            => ??? // remove skill (unselect)
        case _                                 => ???
      }
      commands: Seq[BotCommand] = {
//        expert.info.skills TODO build from selected skills and all known skills
        ???
      }
      //TODO and somehow modify commands of prev msg
    } yield ()
  )

  private val underReview = Node[F](
    "under_review",
    reply("Мы проверяем данные, всё уже почти готово ⏳")
  )

  private val skillsCheckbox = Skill.values.map(_.name).toList.map(s => s"🔲$s")

  private val graph: BotGraph[F] =
    Graph(
      start ~> verifyAndAskName addLabel EqualTo("/start"),
      verifyAndAskName ~> nameAdded addLabel AnyInput,
      nameAdded ~> descriptionAdded addLabel AnyInput,
      descriptionAdded ~> photoAdded addLabel (HasPhoto, 0),
      descriptionAdded ~> descriptionAdded addLabel (AnyInput, 1),
      photoAdded ~> addSkill addLabel OneOf(skillsCheckbox),
      addSkill ~> underReview addLabel (EqualTo(
        "Готово!"
      ), 0, doNothing.some), //TODO we need this override? flush cache here?
      addSkill ~> addSkill addLabel (OneOf(Skill.values.map(_.name).toList), 1), //TODO
      underReview ~> underReview addLabel AnyInput
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
  def apply[F[_]: Sync](implicit
      sttpBackend: SttpBackend[F, Any],
      expertDao: ExpertService[F],
      tg: TelegramClient[F]
  ) =
    new ExpertBot[F]
}
