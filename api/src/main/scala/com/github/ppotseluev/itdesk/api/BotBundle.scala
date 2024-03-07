package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.core.{BotId, BotLogic}

case class BotBundle(
    botType: BotType,
    token: String,
    webhookSecret: WebhookSecret,
    logic: BotLogic
)