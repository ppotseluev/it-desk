package com.github.ppotseluev.itdesk.bots.core.scenario

import com.github.ppotseluev.itdesk.bots.core.Message

object Matcher {
  def isMatched(input: Message.Payload)
               (predicate: ExpectedInputPredicate): Boolean =
    predicate match {
      case ExpectedInputPredicate.TextIsEqualTo(expectedText) =>
        input.text == expectedText
    }
}
