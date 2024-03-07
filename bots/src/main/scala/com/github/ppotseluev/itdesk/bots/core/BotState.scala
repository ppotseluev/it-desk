package com.github.ppotseluev.itdesk.bots.core

import com.github.ppotseluev.itdesk.bots.core.BotDsl.BotScript

case class BotState[F[_]](
    id: BotStateId,
    action: BotScript[F, Unit],
    availableCommands: Seq[BotCommand]
)
