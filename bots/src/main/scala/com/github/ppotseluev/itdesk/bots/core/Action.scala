package com.github.ppotseluev.itdesk.bots.core

sealed trait Action

sealed trait BasicAction extends Action

object Action {

  case class Reply(text: String) extends BasicAction

  case class GoTo(state: BotStateId) extends Action

}
