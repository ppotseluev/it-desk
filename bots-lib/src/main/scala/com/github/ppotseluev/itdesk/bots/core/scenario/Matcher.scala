package com.github.ppotseluev.itdesk.bots.core.scenario

import com.github.ppotseluev.itdesk.bots.Context

object Matcher {
  def isMatched(ctx: Context)(predicate: ExpectedInputPredicate): Boolean =
    predicate match {
      case ExpectedInputPredicate.TextIsEqualTo(expectedText) =>
        ctx.inputText == expectedText
      case ExpectedInputPredicate.AnyInput =>
        true
      case ExpectedInputPredicate.HasPhoto =>
        ctx.inputPhoto.exists(_.nonEmpty)
    }
}
