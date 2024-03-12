package com.github.ppotseluev.itdesk.core.user

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.core.DoobieSerialization._
import com.github.ppotseluev.itdesk.core.user.User.UserSource
import doobie.Transactor
import doobie.implicits._

class MysqlUserDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends UserDao[F] {

  override def upsertUser(user: UserSource): F[Unit] =
    sql"""
             INSERT INTO users (tg_user_id, role)
               VALUES (${user.tgUserId}, ${user.role})
               ON DUPLICATE KEY UPDATE
                  tg_user_id = ${user.tgUserId},
                  role = ${user.role}
           """.update.run.transact(transactor).void

  override def getUser(role: Role, tgUserId: Long): F[Option[User]] =
    sql"""
        SELECT * FROM users
        WHERE role = $role
        AND tg_user_id = $tgUserId
      """
      .query[User]
      .option
      .transact(transactor)
}
