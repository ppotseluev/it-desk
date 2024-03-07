package com.github.ppotseluev.itdesk.bots.core

import cats.free.Free
import cats.free.Free.liftF

//private[core] sealed trait InternalBotDsl[+F[_], T]
//sealed trait BotDsl[+F[_], T] extends InternalBotDsl[F, T]
sealed trait BotDsl[+F[_], T]

object BotDsl {

  /**
   * Free monad based bot's EDSL
   */
  type BotScript[F[_], T] = Free[BotDsl[F, *], T]
//  type BotScript[F[_], T] = Free[InternalBotDsl[F, *], T]
//  type ExecutableBotScript[F[_], T] = Free[BotDsl]

  case object GetCurrentState extends BotDsl[Nothing, Option[BotStateId]]

  case class SaveState(botStateId: BotStateId) extends BotDsl[Nothing, Unit]

  case class Reply(message: Message.Payload) extends BotDsl[Nothing, Unit]

  case class Execute[F[_], T](f: F[T]) extends BotDsl[F, T]

//  case class GoTo(state: BotStateId) extends InternalBotDsl[Nothing, BotStateId]

//  def goTo[F[_]](state: BotStateId): BotScript[F, BotStateId] =
//    liftF(GoTo(state))

  def getCurrentState[F[_]]: BotScript[F, Option[BotStateId]] =
    liftF(GetCurrentState)

  def saveState[F[_]](botStateId: BotStateId): BotScript[F, Unit] =
    liftF(SaveState(botStateId))

  def reply[F[_]](message: Message.Payload): BotScript[F, Unit] =
    liftF(Reply(message))

  def reply[F[_]](text: String): BotScript[F, Unit] =
    reply(Message.Payload(text, Seq.empty))

  def execute[F[_], T](f: F[T]): BotScript[F, T] =
    liftF(Execute(f))

}
