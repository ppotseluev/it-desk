package com.github.ppotseluev.itdesk.bots

import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId

case class Context(
    botId: BotId,
    chatId: ChatId,
    input: String,
    user: TgUser
)

case class TgUser(
    id: Long,
    username: String
)
