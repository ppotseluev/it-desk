package com.github.ppotseluev.itdesk.bots

import com.github.ppotseluev.itdesk.bots.core.BotDsl.BotScript

package object core {
  type BotLogic[F[_]] = CallContext => BotScript[F, Unit]
  type ChatId = String
  type BotStateId = String
  type BotId = String
}
