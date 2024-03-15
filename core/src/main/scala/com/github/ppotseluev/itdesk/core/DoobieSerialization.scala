package com.github.ppotseluev.itdesk.core

import com.github.ppotseluev.itdesk.core.expert.ExpertStatus
import com.github.ppotseluev.itdesk.core.expert.Skill
import com.github.ppotseluev.itdesk.core.user.Role
import doobie.Get
import doobie.implicits.javasql._
import doobie.util.Put
import enumeratum.values.IntEnum
import enumeratum.values.IntEnumEntry
import java.sql.Timestamp
import java.time.Instant

object DoobieSerialization {
  def get[T <: IntEnumEntry](`enum`: IntEnum[T]): Get[T] = Get[Int].tmap(`enum`.withValue)
  def put[T <: IntEnumEntry]: Put[T] = Put[Int].tcontramap(_.value)

  implicit val getTime: Get[Instant] = Get[Timestamp].tmap(_.toInstant)
  implicit val putTime: Put[Instant] = Put[Timestamp].tcontramap(Timestamp.from)
  implicit val getRole: Get[Role] = get(Role)
  implicit val putRole: Put[Role] = put[Role]
  implicit val getExpertStatus: Get[ExpertStatus] = get(ExpertStatus)
  implicit val putExpertStatus: Put[ExpertStatus] = put[ExpertStatus]
  implicit val getSkill: Get[Skill] = get(Skill)
  implicit val putSkill: Put[Skill] = put[Skill]
}
