package com.github.ppotseluev.itdesk.bots.core.scenario

sealed trait ExpectedInputPredicate

object ExpectedInputPredicate {
  case class EqualTo(expectedText: String) extends ExpectedInputPredicate
  case class OneOf(expected: List[String]) extends ExpectedInputPredicate
  case object AnyInput extends ExpectedInputPredicate
  case object HasPhoto extends ExpectedInputPredicate
}
