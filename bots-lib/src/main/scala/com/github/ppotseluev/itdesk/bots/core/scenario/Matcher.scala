package com.github.ppotseluev.itdesk.bots.core.scenario

import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.BotCommand

object Matcher {
  private def commandMatch(ctx: CallContext)(cmd: BotCommand): Boolean =
    cmd match {
      case BotCommand.Regular(text) =>
        ctx.inputText == text
      case BotCommand.Callback(_, callbackData) =>
        ctx.callbackQuery.exists(_.data.contains(callbackData))
    }

  def isMatched(ctx: CallContext)(predicate: ExpectedInputPredicate): Boolean =
    predicate match {
      case ExpectedInputPredicate.EqualTo(cmd)    => commandMatch(ctx)(cmd)
      case ExpectedInputPredicate.AnyInput        => true
      case ExpectedInputPredicate.HasPhoto        => ctx.inputPhoto.exists(_.nonEmpty)
      case ExpectedInputPredicate.OneOf(commands) => commands.exists(commandMatch(ctx))
      case ExpectedInputPredicate.CallbackButton  => ctx.callbackQuery.exists(_.data.isDefined)
    }
}
