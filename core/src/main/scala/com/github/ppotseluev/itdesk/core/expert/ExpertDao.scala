package com.github.ppotseluev.itdesk.core.expert

import com.github.ppotseluev.itdesk.core.user.User

trait ExpertDao[F[_]] {
  def upsertExpert(expert: Expert): F[Unit]
  def getExpert(user: User): F[Option[Expert]]
}
