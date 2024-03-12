package com.github.ppotseluev.itdesk.bots.runtime

import cats.ApplicativeThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.Context
import com.github.ppotseluev.itdesk.bots.core.BotDsl

class BotInterpreterImpl[F[_]: ApplicativeThrow](
    botStateDao: BotStateDao[F],
    chatService: ChatService[F]
)(context: Context)
    extends BotInterpreter[F] {

  def apply[A](botDsl: BotDsl[F, A]): F[A] = {
    import context._
    botDsl match {
      case BotDsl.GetCurrentState =>
        botStateDao.get(chatId -> botId).map(_.map(_.botStateId))
      case BotDsl.SaveState(botStateId) =>
        botStateDao.put(chatId -> botId, BotInfo(botStateId))
      case BotDsl.Reply(message) =>
        chatService.send(botToken)(chatId)(message)
      case BotDsl.Execute(f) =>
        f
      case BotDsl.GetContext() =>
        context.pure[F]
      case BotDsl.RaiseError(botError) =>
        botError.raiseError[F, A]
    }
  }
}
