package com.github.ppotseluev.itdesk.core.expert

import cats.Show
import com.github.ppotseluev.itdesk.core.expert.Expert.Status
import com.github.ppotseluev.itdesk.core.user.User
import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry

case class Expert(
    user: User,
    info: Expert.Info,
    status: Status
)

object Expert {
  implicit val show: Show[Expert] = Show.show { expert =>
    s"""
      |${expert.info.name.getOrElse("")}\n\n
      |${expert.info.description.getOrElse("")}
      |""".stripMargin
  }

  case class Info(
      name: Option[String],
      description: Option[String],
      photo: Option[String]
  )
  object Info {
    val empty: Info = Info(None, None, None)
  }

  sealed abstract class Status(val value: Int) extends IntEnumEntry

  object Status extends IntEnum[Status] {

    /**
     * Expert is registerd in the platform but his profile is not visible for the students
     */
    case object New extends Status(0)

    /**
     * Approved expert who is ready to accept students
     */
    case object Active extends Status(1)

    override val values: IndexedSeq[Status] = findValues
  }

}
