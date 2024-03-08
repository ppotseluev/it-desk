package com.github.ppotseluev.itdesk.bots.core

import cats.free.Free
import cats.free.Free.liftF
import cats.~>

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

  private[bots] case object GetCurrentState extends BotDsl[Nothing, Option[BotStateId]]

  private[bots] case class SaveState(botStateId: BotStateId) extends BotDsl[Nothing, Unit]

  private[bots] case class Reply(message: Message.Payload) extends BotDsl[Nothing, Unit]

  private[bots] case class Execute[F[_], T](f: F[T]) extends BotDsl[F, T]

  private class Enricher[F[_]](commands: List[BotCommand]) extends (BotDsl[F, *] ~> BotDsl[F, *]) {
    override def apply[A](fa: BotDsl[F, A]): BotDsl[F, A] = fa match {
      case Reply(message) => Reply(message.copy(availableCommands = commands))
      case x              => x
    }
  }

  private[bots] implicit class BotScriptSyntax[F[_], T](val script: BotScript[F, T])
      extends AnyVal {
    def withAvailableCommands(commands: List[BotCommand]): Free[BotDsl[F, *], T] =
      script.mapK(new Enricher[F](commands))
  }

  private[bots] def getCurrentState[F[_]]: BotScript[F, Option[BotStateId]] =
    liftF(GetCurrentState)

  private[bots] def saveState[F[_]](botStateId: BotStateId): BotScript[F, Unit] =
    liftF(SaveState(botStateId))

  def reply[F[_]](text: String): BotScript[F, Unit] =
    liftF(Reply(Message.Payload(text, Seq.empty)))

  def execute[F[_], T](f: F[T]): BotScript[F, T] =
    liftF(Execute(f))

}
