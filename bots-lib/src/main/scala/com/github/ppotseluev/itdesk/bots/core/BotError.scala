package com.github.ppotseluev.itdesk.bots.core

sealed trait BotError

object BotError {
  case object AccessDenied extends BotError
}
