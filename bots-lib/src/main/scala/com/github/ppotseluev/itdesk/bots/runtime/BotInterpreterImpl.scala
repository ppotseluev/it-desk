package com.github.ppotseluev.itdesk.bots.runtime

import cats.ApplicativeThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotDsl
import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId

class BotInterpreterImpl[F[_]: ApplicativeThrow](
    botStateDao: BotStateDao[F],
    chatService: ChatService[F],
    botToken: BotId => String //todo take from ctx?
)(context: InterpreterContext)
    extends BotInterpreter[F] {

  def apply[A](botDsl: BotDsl[F, A]): F[A] = {
    import context._
    botDsl match {
      case BotDsl.GetCurrentState =>
        botStateDao.get(chatId -> botId).map(_.map(_.botStateId))
      case BotDsl.SaveState(botStateId) =>
        botStateDao.put(chatId -> botId, BotInfo(botStateId))
      case BotDsl.Reply(message) =>
        chatService.send(botToken(botId))(chatId)(message)
      case BotDsl.Execute(f) =>
        f
      case BotDsl.GetInput() =>
        input.pure[F]
    }
  }
}

case class InterpreterContext(botId: BotId, chatId: ChatId, input: String)
