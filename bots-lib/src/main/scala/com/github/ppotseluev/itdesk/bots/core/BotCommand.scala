package com.github.ppotseluev.itdesk.bots.core

sealed trait BotCommand
object BotCommand {
  def apply(text: String): Regular = Regular(text)
  case class Regular(text: String) extends BotCommand
  case class Callback(text: String, callbackData: String) extends BotCommand
}
