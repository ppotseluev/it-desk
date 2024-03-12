package com.github.ppotseluev.itdesk.core.expert

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.core.DoobieSerialization._
import com.github.ppotseluev.itdesk.core.expert.MysqlExpertDao.ExpertRecord
import com.github.ppotseluev.itdesk.core.user.User
import doobie.Transactor
import doobie.implicits._

class MysqlExpertDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends ExpertDao[F] {

  override def upsertExpert(expert: Expert): F[Unit] = {
    import expert.info
    sql"""
             INSERT INTO experts (user_id, name, description, status)
               VALUES (${expert.user.id}, ${info.name}, ${info.description}, ${expert.status})
               ON DUPLICATE KEY UPDATE
                  name = ${info.name},
                  description = ${info.description},
                  status = ${expert.status},
                  photo = ${info.photo}
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

object MysqlExpertDao {
  case class ExpertRecord(
      userId: Long,
      name: Option[String],
      description: Option[String],
      status: Expert.Status,
      photo: Option[String]
  ) {
    def expert(user: User): Expert = Expert(
      user = user,
      info = Expert.Info(name = name, description = description, photo = photo),
      status = status
    )
  }
}
