package com.github.ppotseluev.itdesk.core.user

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.github.ppotseluev.itdesk.core.DoobieSerialization._
import com.github.ppotseluev.itdesk.core.user.User.UserSource
import com.github.ppotseluev.itdesk.core.user.UserDao.Filter
import doobie.Transactor
import doobie.implicits._

class PgUserDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends UserDao[F] {

  override def upsertUser(user: UserSource): F[Unit] =
    sql"""
             INSERT INTO it_desk_users (tg_user_id, role)
               VALUES (${user.tgUserId}, ${user.role})
               ON CONFLICT (tg_user_id, role) DO NOTHING
           """.update.run.transact(transactor).void

  override def getUser(role: Role, tgUserId: Long): F[Option[User]] =
    sql"""
        SELECT * FROM it_desk_users
        WHERE role = $role
        AND tg_user_id = $tgUserId
      """
      .query[User]
      .option
      .transact(transactor)

  override def getUsers(filter: UserDao.Filter): F[Vector[User]] = filter match {
    case Filter.All =>
      sql"""
          SELECT * FROM it_desk_users
        """
        .query[User]
        .to[Vector]
        .transact(transactor)
  }
}
