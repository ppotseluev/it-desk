package com.github.ppotseluev.itdesk.bots

import com.github.ppotseluev.itdesk.bots.core.BotDsl.BotScript

package object core {
  type BotLogic = BotInput => BotScript[Unit]
  type ChatId = String
  type BotStateId = String
  type BotCommand = String
  type BotId = String
}
