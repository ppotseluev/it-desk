package com.github.ppotseluev.itdesk.api

import com.github.ppotseluev.itdesk.api.telegram.WebhookSecret
import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.storage.MySqlConfig

case class Config(
    telegramChatId: String,
    telegramUrl: String,
    telegramUsersWhitelist: Set[Int],
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

  def telegramTrackedChats: Set[String] = Set(telegramChatId)
}

case class BotConfig(
    token: String,
    webhookSecret: WebhookSecret
)
