package com.github.ppotseluev.itdesk.serialization

import cats.Contravariant

trait StringEncoder[T] {
  def write(obj: T): String
}

object StringEncoder {
  implicit val contravariant: Contravariant[StringEncoder] = new Contravariant[StringEncoder] {
    override def contramap[A, B](fa: StringEncoder[A])
                                (f: B => A): StringEncoder[B] =
      (obj: B) => fa.write(f(obj))
  }
}