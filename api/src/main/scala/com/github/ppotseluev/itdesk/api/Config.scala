package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.storage.MySqlConfig

case class Config(
    telegramUrl: String,
    apiConfig: Api.Config,
    dbConfig: MySqlConfig,
    botStatesTable: String,
    bots: Map[BotType, BotConfig],
    localEnv: Boolean = false
) {
  def botWithId(id: BotId): BotConfig = {
    val botType = BotType.values
      .find(_.id == id)
      .getOrElse(throw new NoSuchElementException(s"Bot $id not found"))
    bots(botType)
  }
}

case class BotConfig(
    token: String,
    webhookSecret: WebhookSecret,
    chatId: Option[ChatId]
)
