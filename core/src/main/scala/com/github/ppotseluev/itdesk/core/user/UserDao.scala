package com.github.ppotseluev.itdesk.core.user

import User.UserSource

trait UserDao[F[_]] {
  def upsertUser(user: UserSource): F[Unit]

  def getUser(role: Role, tgUserId: Long): F[Option[User]]
}
