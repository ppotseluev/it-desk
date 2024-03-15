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

  private[bots] case class RaiseError(botError: BotError) extends BotDsl[Nothing, Unit]

  private class Enricher[F[_]](commands: List[BotCommand]) extends (BotDsl[F, *] ~> BotDsl[F, *]) {
    override def apply[A](fa: BotDsl[F, A]): BotDsl[F, A] = fa match {
      case Reply(message) => Reply(message.copy(availableCommands = commands))
      case x              => x
    }
  }

  private[bots] implicit class BotScriptSyntax[F[_], T](val script: BotScript[F, T])
      extends AnyVal {
    //TODO apply this override only if 'original' commands are not present?
    def withAvailableCommands(commands: List[BotCommand]): Free[BotDsl[F, *], T] =
      script.mapK(new Enricher[F](commands))
  }

  private[bots] def getCurrentState[F[_]]: BotScript[F, Option[BotStateId]] =
    liftF(GetCurrentState)

  private[bots] def saveState[F[_]](botStateId: BotStateId): BotScript[F, Unit] =
    liftF(SaveState(botStateId))

  def reply[F[_]](
      text: String,
      photo: Option[Either[String, Array[Byte]]] = None
  ): BotScript[F, Unit] =
    liftF(Reply(Message(Payload(text, photo), Seq.empty)))

  def execute[F[_], T](f: F[T]): BotScript[F, T] = liftF(Execute(f))

  def getCallContext[F[_]]: BotScript[F, CallContext] = liftF(GetCallContext)

  def getInput[F[_]]: BotScript[F, String] = getCallContext.map(_.inputText)

  def doNothing[F[_]]: BotScript[F, Unit] = ().pure[BotScript[F, *]]

  def raiseError[F[_]](botError: BotError): BotScript[F, Unit] =
    liftF(RaiseError(botError))
}
