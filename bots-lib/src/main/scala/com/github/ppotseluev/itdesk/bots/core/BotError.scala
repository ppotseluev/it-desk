package com.github.ppotseluev.itdesk.bots.core

sealed trait BotError extends RuntimeException

object BotError {
  case object AccessDenied extends BotError
}
