package com.github.ppotseluev.itdesk.bots.core.scenario

sealed trait ExpectedInputPredicate

object ExpectedInputPredicate {
  case class TextIsEqualTo(expectedText: String) extends ExpectedInputPredicate
  case object AnyInput extends ExpectedInputPredicate
  case object HasPhoto extends ExpectedInputPredicate
}
