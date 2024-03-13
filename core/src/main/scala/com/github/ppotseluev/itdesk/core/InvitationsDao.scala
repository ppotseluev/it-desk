package com.github.ppotseluev.itdesk.core

import com.github.ppotseluev.itdesk.core.user.Role

trait InvitationsDao[F[_]] {
  def upsertInvite(invite: Invite): F[Unit]
  def getInvite(tgUsername: String, role: Role): F[Option[Invite]]
}
