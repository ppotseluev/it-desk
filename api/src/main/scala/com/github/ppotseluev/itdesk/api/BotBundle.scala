package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.core.BotLogic
import com.github.ppotseluev.itdesk.bots.core.ChatId

case class BotBundle[F[_]](
    botType: BotType,
    token: String,
    webhookSecret: WebhookSecret,
    logic: BotLogic[F],
    chatId: Option[ChatId]
)
