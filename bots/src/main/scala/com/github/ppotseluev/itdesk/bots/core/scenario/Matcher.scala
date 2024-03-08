package com.github.ppotseluev.itdesk.bots.core.scenario

object Matcher {
  def isMatched(input: String)(predicate: ExpectedInputPredicate): Boolean =
    predicate match {
      case ExpectedInputPredicate.TextIsEqualTo(expectedText) =>
        input == expectedText
    }
}
