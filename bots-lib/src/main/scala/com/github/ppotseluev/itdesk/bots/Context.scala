package com.github.ppotseluev.itdesk.bots

import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId

case class Context(
    botToken: String,
    botId: BotId,
    chatId: ChatId,
    inputText: String,
    inputPhoto: Option[List[TgPhoto]],
    user: TgUser
)

case class TgUser(
    id: Long,
    username: String
)

case class TgPhoto(
    fileId: String,
    fileUniqueId: String,
    fileSize: Int,
    width: Int,
    height: Int
)
