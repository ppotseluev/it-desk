package com.github.ppotseluev.itdesk.bots.core.scenario

import com.github.ppotseluev.itdesk.bots.core.BotCommand

sealed trait ExpectedInputPredicate

object ExpectedInputPredicate {
  def equalTo(text: String): EqualTo = EqualTo(BotCommand.Regular(text))
  case class EqualTo(expected: BotCommand) extends ExpectedInputPredicate
  case class OneOf(expected: List[BotCommand]) extends ExpectedInputPredicate
  case object AnyInput extends ExpectedInputPredicate
  case object HasPhoto extends ExpectedInputPredicate
  case object CallbackButton extends ExpectedInputPredicate
}
