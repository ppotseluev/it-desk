package com.github.ppotseluev.itdesk.core.expert

import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry

sealed abstract class ExpertStatus(val value: Int) extends IntEnumEntry

object ExpertStatus extends IntEnum[ExpertStatus] {

  /**
   * Expert is registered in the platform but his profile is not visible for the students
   */
  case object New extends ExpertStatus(0)

  /**
   * Approved expert who is ready to accept students
   */
  case object Active extends ExpertStatus(1)

  override val values: IndexedSeq[ExpertStatus] = findValues
}
