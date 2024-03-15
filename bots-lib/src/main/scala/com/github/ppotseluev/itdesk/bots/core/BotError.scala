package com.github.ppotseluev.itdesk.bots.core

sealed abstract class BotError(val msg: String) extends RuntimeException(msg)

object BotError {
  def missedField(fieldName: String): IllegalInput = IllegalInput(s"Missed field '$fieldName'")
  case object AccessDenied extends BotError("Access denied")
  case class IllegalInput(override val msg: String) extends BotError(msg)
}
