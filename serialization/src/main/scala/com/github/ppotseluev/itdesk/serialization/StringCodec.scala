package com.github.ppotseluev.itdesk.serialization

import cats.Invariant
import cats.syntax.contravariant._
import cats.syntax.functor._

trait StringCodec[T] extends StringDecoder[T] with StringEncoder[T]

object StringCodec {
  def apply[T](implicit codec: StringCodec[T]): StringCodec[T] =
    codec

  def from[T](decoder: StringDecoder[T],
              encoder: StringEncoder[T]): StringCodec[T] =
    new StringCodec[T] {
      override def read(str: String): Either[String, T] =
        decoder.read(str)

      override def write(obj: T): String =
        encoder.write(obj)
    }

  implicit val invariant: Invariant[StringCodec] = new Invariant[StringCodec] {
    override def imap[A, B](fa: StringCodec[A])
                           (f: A => B)
                           (g: B => A): StringCodec[B] =
      StringCodec.from(
        decoder = (fa: StringDecoder[A]).map(f),
        encoder = (fa: StringEncoder[A]).contramap(g)
      )
  }
}