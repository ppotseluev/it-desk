package com.github.ppotseluev.itdesk.core.expert

import cats.effect.kernel.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.core.InvitationsDao
import com.github.ppotseluev.itdesk.core.model.Invite
import com.github.ppotseluev.itdesk.core.model.Role
import java.time.Instant
import scala.concurrent.duration._

trait ExpertService[F[_]] {
  def inviteExpert(tgUsername: String, validFor: FiniteDuration = 2.days): F[Invite]

  def getInvite(tgUsername: String): F[Option[Invite]]
}

object ExpertService {
  def apply[F[_]: Sync](implicit invitationsDao: InvitationsDao[F]): ExpertService[F] =
    new ExpertService[F] {
      override def inviteExpert(tgUsername: String, validFor: FiniteDuration): F[Invite] =
        Sync[F].delay(Instant.now).flatMap { time =>
          val invite = Invite(
            tgUsername = tgUsername,
            role = Role.Expert,
            validUntil = time.plusSeconds(validFor.toSeconds)
          )
          invitationsDao.upsertInvite(invite).as(invite)
        }

      override def getInvite(tgUsername: String): F[Option[Invite]] =
        invitationsDao.getInvite(tgUsername, Role.Expert)
    }
}
