package com.github.ppotseluev.itdesk.core

import cats.effect.MonadCancelThrow
import cats.syntax.functor._
import com.github.ppotseluev.itdesk.core.model.Invite
import com.github.ppotseluev.itdesk.core.model.Role
import doobie.Transactor
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.util.update.Update
import java.sql.Timestamp

class MysqlInvitationsDao[F[_]](implicit
    transactor: Transactor[F],
    F: MonadCancelThrow[F]
) extends InvitationsDao[F] {
  import com.github.ppotseluev.itdesk.core.MysqlInvitationsDao._

  def upsertInvite(invite: Invite): F[Unit] = {
    val record = InviteRecord.fromInvite(invite)
    sql"""
         INSERT INTO invitations (tg_username, role, valid_until)
           VALUES (${record.tgUsername}, ${record.role}, ${record.validUntil})
           ON DUPLICATE KEY UPDATE
              valid_until = ${record.validUntil}
       """.update.run.transact(transactor).void
  }

  def getInvite(tgUsername: String, role: Role): F[Option[Invite]] =
    sql"""
      SELECT * FROM invitations
      WHERE tg_username = $tgUsername
      AND role = ${role.value}
    """
      .query[InviteRecord]
      .option
      .transact(transactor)
      .map(_.map(_.toInvite))
}

object MysqlInvitationsDao {
  private case class InviteRecord(
      tgUsername: String,
      role: Int,
      validUntil: Timestamp
  ) {
    def toInvite: Invite = Invite(
      tgUsername = tgUsername,
      role = Role.withValue(role),
      validUntil = validUntil.toInstant
    )
  }

  private object InviteRecord {
    def fromInvite(invite: Invite): InviteRecord = InviteRecord(
      tgUsername = invite.tgUsername,
      role = invite.role.value,
      validUntil = Timestamp.from(invite.validUntil)
    )
  }
}
