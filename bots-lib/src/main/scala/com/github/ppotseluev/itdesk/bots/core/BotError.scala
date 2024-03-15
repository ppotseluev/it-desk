package com.github.ppotseluev.itdesk.bots.core

sealed abstract class BotError(val msg: String) extends RuntimeException(msg)

object BotError {
  case object AccessDenied extends BotError("Access denied")
  case class IllegalInput(override val msg: String) extends BotError(msg)
}
