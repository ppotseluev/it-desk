package com.github.ppotseluev.itdesk.core.expert

import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry

sealed abstract class Skill(val value: Int, val name: String) extends IntEnumEntry

object Skill extends IntEnum[Skill] {
  case object Java extends Skill(0, "Java")
  case object Python extends Skill(1, "Python")
  case object JavaScript extends Skill(2, "JavaScript")

  def withName(name: String): Option[Skill] =
    values.find(_.name == name)

  override val values: IndexedSeq[Skill] = findValues
}
