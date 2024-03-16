package com.github.ppotseluev.itdesk.core.expert

import cats.Show
import com.github.ppotseluev.itdesk.core.user.User

case class Expert(
    user: User,
    info: Expert.Info,
    status: ExpertStatus
)

object Expert {
  implicit val show: Show[Expert] = Show.show { expert =>
    s"""
      |${expert.info.name.getOrElse("")}\n
      |С чем готов помочь: ${expert.info.skills.toSet.flatten.map(_.name).mkString(", ")}\n
      |${expert.info.description.getOrElse("")}
      |""".stripMargin
  }

  case class Info(
      name: Option[String],
      description: Option[String],
      photo: Option[Array[Byte]],
      skills: Option[Set[Skill]]
  )
  object Info {
    val empty: Info = Info(None, None, None, None)
  }
}
