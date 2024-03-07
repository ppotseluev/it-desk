package com.github.ppotseluev.itdesk.serialization

import cats.Functor

trait StringDecoder[T] {
  def read(str: String): Either[String, T]
}

object StringDecoder {
  implicit val functor: Functor[StringDecoder] = new Functor[StringDecoder] {
    override def map[A, B](fa: StringDecoder[A])
                          (f: A => B): StringDecoder[B] =
      (str: String) => fa.read(str).map(f)
  }
}