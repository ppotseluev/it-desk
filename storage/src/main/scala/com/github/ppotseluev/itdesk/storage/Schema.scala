package com.github.ppotseluev.itdesk.storage

import com.github.ppotseluev.itdesk.serialization.StringCodec
import io.circe.Codec

sealed trait Schema[T]

object Schema {
  case class Json[T](codec: Codec[T]) extends Schema[T]

  case class String[T](codec: StringCodec[T]) extends Schema[T]
}
