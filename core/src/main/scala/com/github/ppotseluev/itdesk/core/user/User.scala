package com.github.ppotseluev.itdesk.core.user

case class User(
    id: Long,
    tgUserId: Long,
    role: Role
)

object User {
  case class UserSource(tgUserId: Long, role: Role)
}
