package com.github.ppotseluev.itdesk.core.expert

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.core.DoobieSerialization._
import com.github.ppotseluev.itdesk.core.expert.PgExpertDao.ExpertRecord
import com.github.ppotseluev.itdesk.core.user.User
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._

class PgExpertDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends ExpertDao[F] {

  override def upsertExpert(expert: Expert): F[Unit] = {
    import expert.info
    sql"""
             INSERT INTO experts (user_id, name, description, status)
               VALUES (${expert.user.id}, ${info.name}, ${info.description}, ${expert.status})
               ON CONFLICT (user_id) DO UPDATE SET
                  name = ${info.name},
                  description = ${info.description},
                  status = ${expert.status},
                  photo = ${info.photo},
                  skills = ${info.skills.toList.flatten.map(_.value)}
           """.update.run.transact(transactor).void
  }

  override def getExpert(user: User): F[Option[Expert]] =
    sql"""
        SELECT * FROM experts
        WHERE user_id = ${user.id}
      """
      .query[ExpertRecord]
      .map(_.expert(user))
      .option
      .transact(transactor)
}

object PgExpertDao {
  case class ExpertRecord(
      userId: Long,
      name: Option[String],
      description: Option[String],
      status: ExpertStatus,
      photo: Option[Array[Byte]],
      skills: List[Int]
  ) {
    def expert(user: User): Expert = Expert(
      user = user,
      info = Expert.Info(
        name = name,
        description = description,
        photo = photo,
        skills = skills.map(_.toInt).map(Skill.withValue).toSet.some
      ),
      status = status
    )
  }
}
