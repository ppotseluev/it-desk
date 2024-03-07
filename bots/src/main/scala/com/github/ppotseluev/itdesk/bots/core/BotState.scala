package com.github.ppotseluev.itdesk.bots.core

case class BotState(id: BotStateId, action: BasicAction, availableCommands: Seq[BotCommand])
