package com.github.ppotseluev.itdesk.bots.runtime

import cats.ApplicativeError
import cats.implicits._
import com.github.ppotseluev.itdesk.bots.core.BotDsl
import com.github.ppotseluev.itdesk.bots.core.BotId

class BotInterpreterImpl[F[_]](
    botStateDao: BotStateDao[F],
    chatService: ChatService[F],
    botToken: BotId => String
)(implicit
    F: ApplicativeError[F, Throwable]
) extends BotInterpreter[F] {

  override def apply[A](botDsl: BotDsl[A]): F[A] = botDsl match {
    case BotDsl.GetCurrentState(chatId, botId) =>
      botStateDao.get(chatId -> botId).map(_.map(_.botStateId))
    case BotDsl.SaveState(chatId, botId, botStateId) =>
      botStateDao.put(chatId -> botId, BotInfo(botStateId))
    case BotDsl.Reply(botId, chatId, message) =>
      chatService.send(botToken(botId))(chatId)(message)
  }
}
