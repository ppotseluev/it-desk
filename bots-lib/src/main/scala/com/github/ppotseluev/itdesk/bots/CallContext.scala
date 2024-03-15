package com.github.ppotseluev.itdesk.bots

import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.bots.telegram.TelegramModel._

case class CallContext(
    botToken: String,
    botId: BotId,
    chatId: ChatId,
    inputText: String,
    inputPhoto: Option[List[Photo]],
    user: User,
    callbackQuery: Option[CallbackQuery]
)
