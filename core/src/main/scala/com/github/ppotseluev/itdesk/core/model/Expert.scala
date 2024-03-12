package com.github.ppotseluev.itdesk.core.model

import com.github.ppotseluev.itdesk.core.model.Expert.Status
import enumeratum.Enum
import enumeratum.EnumEntry

case class Expert(
    user: User,
    name: String,
    status: Status
)

object Expert {
  sealed trait Status extends EnumEntry

  object Status extends Enum[Status] {

    /**
     * Expert is registerd in the platform but his profile is not visible for the students
     */
    case object New extends Status

    /**
     * Approved expert who is ready to accept students
     */
    case object Active extends Status

    override val values: IndexedSeq[Status] = findValues
  }

}
