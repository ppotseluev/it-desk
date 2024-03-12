package com.github.ppotseluev.itdesk.core.user

import User.UserSource
import com.github.ppotseluev.itdesk.core.user.UserDao.Filter

trait UserDao[F[_]] {
  def upsertUser(user: UserSource): F[Unit]

  def getUser(role: Role, tgUserId: Long): F[Option[User]]

  def getUsers(filter: Filter): F[Vector[User]]
}

object UserDao {
  sealed trait Filter
  object Filter {
    case object All extends Filter
  }
}
