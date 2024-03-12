package com.github.ppotseluev.itdesk.core.expert

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.core.DoobieSerialization._
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
                  status = ${expert.status}
           """.update.run.transact(transactor).void
  }

  override def getExpert(userId: Long): F[Option[Expert]] =
    sql"""
        SELECT * FROM experts
        WHERE user_id = $userId
      """
      .query[Expert]
      .option
      .transact(transactor)
}
