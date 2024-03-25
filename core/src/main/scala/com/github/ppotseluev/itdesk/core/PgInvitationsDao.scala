package com.github.ppotseluev.itdesk.core

import cats.effect.MonadCancelThrow
import cats.syntax.functor._
import com.github.ppotseluev.itdesk.core.user.Role
import doobie._
import doobie.implicits._

import DoobieSerialization._

class PgInvitationsDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends InvitationsDao[F] {
  import PgInvitationsDao._

  def upsertInvite(invite: Invite): F[Unit] = {
    sql"""
         INSERT INTO $table (tg_username, role, valid_until)
           VALUES (${invite.tgUsername}, ${invite.role}, ${invite.validUntil})
           ON CONFLICT (tg_username, role) DO UPDATE SET
              valid_until = ${invite.validUntil}
       """.update.run.transact(transactor).void
  }

  def getInvite(tgUsername: String, role: Role): F[Option[Invite]] =
    sql"""
      SELECT * FROM $table
      WHERE tg_username = $tgUsername
      AND role = ${role.value}
    """
      .query[Invite]
      .option
      .transact(transactor)
}

object PgInvitationsDao {
  val table = "it_desk_invitations"
}
