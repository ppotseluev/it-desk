package com.github.ppotseluev.itdesk.bots.runtime

import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.bots.core.Message

trait ChatService[F[_]] {
  def send(botToken: String)(chatId: ChatId)(payload: Message.Payload): F[Unit]
}
