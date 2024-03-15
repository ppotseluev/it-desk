package com.github.ppotseluev.itdesk.bots.core.scenario

import com.github.ppotseluev.itdesk.bots.CallContext

object Matcher {
  def isMatched(ctx: CallContext)(predicate: ExpectedInputPredicate): Boolean =
    predicate match {
      case ExpectedInputPredicate.EqualTo(expectedText) =>
        ctx.inputText == expectedText
      case ExpectedInputPredicate.AnyInput =>
        true
      case ExpectedInputPredicate.HasPhoto =>
        ctx.inputPhoto.exists(_.nonEmpty)
      case ExpectedInputPredicate.OneOf(options) =>
        options.contains(ctx.inputText)
    }
}
