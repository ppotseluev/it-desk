package com.github.ppotseluev.itdesk.core.expert

trait ExpertDao[F[_]] {
  def upsertExpert(expert: Expert): F[Unit]
  def getExpert(userId: Long): F[Option[Expert]]
}
