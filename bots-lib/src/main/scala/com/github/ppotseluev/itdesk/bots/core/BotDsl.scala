package com.github.ppotseluev.itdesk.bots.core

import cats.free.Free
import cats.free.Free.liftF
import cats.implicits._
import cats.~>
import com.github.ppotseluev.itdesk.bots.CallContext
import com.github.ppotseluev.itdesk.bots.core.Message.Payload

sealed trait BotDsl[+F[_], T]

object BotDsl {

  /**
   * Free monad based bot's EDSL
   */
  type BotScript[F[_], T] = Free[BotDsl[F, *], T]

  private[bots] case object GetCurrentState extends BotDsl[Nothing, Option[BotStateId]]

  private[bots] case class SaveState(botStateId: BotStateId) extends BotDsl[Nothing, Unit]

  private[bots] case class Reply(message: Message) extends BotDsl[Nothing, Unit]

  private[bots] case class Execute[F[_], T](f: F[T]) extends BotDsl[F, T]

  private[bots] case object GetCallContext extends BotDsl[Nothing, CallContext]

  private[bots] case class RaiseError[T](botError: BotError) extends BotDsl[Nothing, T]

  private class Enricher[F[_]](commands: List[BotCommand]) extends (BotDsl[F, *] ~> BotDsl[F, *]) {
    override def apply[A](fa: BotDsl[F, A]): BotDsl[F, A] = fa match {
      case Reply(message) if message.availableCommands.isEmpty =>
        Reply(message.copy(availableCommands = commands))
      case x => x
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

  def reply[F[_]](
      text: String,
      photo: Option[Either[String, Array[Byte]]] = None,
      availableCommands: Seq[BotCommand] = Seq.empty
  ): BotScript[F, Unit] =
    liftF(Reply(Message(Payload(text, photo), availableCommands)))

  def lift[F[_], T](f: F[T]): BotScript[F, T] = liftF(Execute(f))

  def getCallContext[F[_]]: BotScript[F, CallContext] = liftF(GetCallContext)

  def getInput[F[_]]: BotScript[F, String] = getCallContext.map(_.inputText)

  def doNothing[F[_]]: BotScript[F, Unit] = ().pure[BotScript[F, *]]

  def raiseError[F[_], T](botError: BotError): BotScript[F, T] =
    liftF(RaiseError(botError))

  def getOrFail[F[_], T](fieldName: String, f: CallContext => Option[T]): BotScript[F, T] =
    getCallContext[F].flatMap { ctx =>
      f(ctx) match {
        case Some(value) => value.pure[BotScript[F, *]]
        case None        => raiseError[F, T](BotError.missedField(fieldName))
      }
    }
}
