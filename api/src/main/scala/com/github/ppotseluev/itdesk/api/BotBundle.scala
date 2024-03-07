package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.core.BotLogic

case class BotBundle[F[_]](
    botType: BotType,
    token: String,
    webhookSecret: WebhookSecret,
    logic: BotLogic[F]
)
