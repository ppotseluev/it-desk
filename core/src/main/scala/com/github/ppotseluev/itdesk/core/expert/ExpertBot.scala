package com.github.ppotseluev.itdesk.core.expert

import cats.effect.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.Bot
import com.github.ppotseluev.itdesk.bots.core.Bot.FallbackPolicy
import com.github.ppotseluev.itdesk.bots.core.BotCommand
import com.github.ppotseluev.itdesk.bots.core.BotDsl._
import com.github.ppotseluev.itdesk.bots.core.BotError.AccessDenied
import com.github.ppotseluev.itdesk.bots.core.BotError.IllegalInput
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.AnyInput
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.EqualTo
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.HasPhoto
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.OneOf
import com.github.ppotseluev.itdesk.bots.core.scenario.ExpectedInputPredicate.equalTo
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario
import com.github.ppotseluev.itdesk.bots.core.scenario.GraphBotScenario._
import com.github.ppotseluev.itdesk.bots.telegram.TelegramChatService
import com.github.ppotseluev.itdesk.bots.telegram.TelegramClient
import com.github.ppotseluev.itdesk.bots.telegram.TelegramModel.KeyboardUpdate
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
      username <- getOrFail("username", _.user.username)
      isInviteValid <- hasValidInvite(username, time)
      ctx <- getCallContext
      _ <-
        if (isInviteValid) registerUser(ctx) >> greet
        else reply[F]("Access denied") >> raiseError(AccessDenied)
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

  private def updateInfo(f: (CallContext, Expert.Info) => Expert.Info): BotScript[F, Expert] =
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
      reply("Теперь загрузи фото")
  )
  private val photoAdded = Node[F](
    "add_photo",
    (getCallContext[F] >>= getPhoto).flatMap { file =>
      updateInfo(photo(file))
    } >> reply(
      "Выбери, по каким темам ты будешь проводить консультации. Отметь все подходящие варианты"
    )
  )

  private val getExpert: BotScript[F, Expert] =
    for {
      ctx <- getCallContext
      expert <- execute(expertService.getExpert(ctx.user.id))
    } yield expert

  private def updateSkillsScript(
      f: Set[Skill] => Set[Skill]
  ): BotScript[F, Set[Skill]] =
    for {
      expert <- getExpert
      updatedExpert <- updateInfo { (_, i) =>
        val skills = expert.info.skills.toSet.flatten
        i.copy(skills = f(skills).some)
      }
    } yield updatedExpert.info.skills.toSet.flatten

  private val finishCommand = BotCommand.Callback(
    text = "Отправить",
    callbackData = "."
  )
  private val initialSkillsCheckbox = buildSkillsKeyboard(selectedSkills = Set.empty)

  private def buildSkillsKeyboard(
      selectedSkills: Set[Skill]
  ): List[BotCommand.Callback] =
    Skill.values.map { skill =>
      if (selectedSkills.contains(skill))
        BotCommand.Callback(
          text = s"☑️${skill.name}",
          callbackData = s"-${skill.value}"
        )
      else
        BotCommand.Callback(
          text = s"🔲${skill.name}",
          callbackData = s"+${skill.value}"
        )
    }.toList :+ finishCommand

  private val hideSkillsKeyboard: BotScript[F, Unit] =
    editSkillsKeyboard(Nil)

  private def editSkillsKeyboard(newKeyboard: List[BotCommand.Callback]): BotScript[F, Unit] =
    for {
      messageId <- getOrFail("callback.message_id", _.callbackQuery.map(_.message.messageId))
      ctx <- getCallContext
      markup <- execute(TelegramChatService.buildKeyboard[F](newKeyboard))
      keyboardUpdate = KeyboardUpdate(
        chatId = ctx.chatId,
        messageId = messageId,
        replyMarkup = markup
      )
      _ <- execute {
        tg.editInlineKeyboard(ctx.botToken, keyboardUpdate)
      }
    } yield ()

  private val addSkill = Node[F](
    "add_skill",
    for {
      input <- getOrFail("callback_data", _.callbackQuery.flatMap(_.data))
      (action, skillNumber) = (input.head, input.tail)
      _ <- action match {
        case '+' | '-' =>
          val skill = Skill.withValue(skillNumber.toInt) //TODO it's unsafe
          val updateSkills = action match {
            case '+' => updateSkillsScript(_ + skill)
            case '-' => updateSkillsScript(_ - skill)
          }
          updateSkills.map(buildSkillsKeyboard) >>= editSkillsKeyboard
        case x => raiseError[F, Unit](IllegalInput(s"Unexpected input '$x'"))
      }
    } yield ()
  )

  private val underReview = Node[F](
    "under_review",
    reply(
      "Благодарим за заполнение анкеты! Мы уже проверяем данные и скоро активируем твой профиль ⏳"
    )
  )

  private val graph: BotGraph[F] =
    Graph(
      start ~> verifyAndAskName addLabel equalTo("/start"),
      verifyAndAskName ~> nameAdded addLabel AnyInput,
      nameAdded ~> descriptionAdded addLabel AnyInput,
      descriptionAdded ~> photoAdded addLabel (HasPhoto, 0),
      descriptionAdded ~> descriptionAdded addLabel (AnyInput, 1),
      photoAdded ~> addSkill addLabel OneOf(initialSkillsCheckbox),
      addSkill ~> underReview addLabel (
        EqualTo(finishCommand),
        order = 0,
        actionOverride = (hideSkillsKeyboard >> underReview.action).some
      ),
      addSkill ~> addSkill addLabel (AnyInput, 1),
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
