package com.github.ppotseluev.itdesk.core.user

import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry

sealed abstract class Role(val value: Int) extends IntEnumEntry

object Role extends IntEnum[Role] {
  case object Expert extends Role(0)
  case object Student extends Role(1)

  override val values: IndexedSeq[Role] = findValues
}
