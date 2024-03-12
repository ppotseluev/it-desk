package com.github.ppotseluev.itdesk.core.expert

import cats.effect.kernel.Sync
import cats.implicits._
import com.github.ppotseluev.itdesk.core
import com.github.ppotseluev.itdesk.core.InvitationsDao
import com.github.ppotseluev.itdesk.core.Invite
import com.github.ppotseluev.itdesk.core.user.Role
import com.github.ppotseluev.itdesk.core.user.User
import com.github.ppotseluev.itdesk.core.user.User.UserSource
import com.github.ppotseluev.itdesk.core.user.UserDao
import java.time.Instant
import scala.concurrent.duration._

trait ExpertService[F[_]] {
  def register(tgUserId: Long): F[Unit]

  def inviteExpert(tgUsername: String, validFor: FiniteDuration = 2.days): F[Invite]

  def getInvite(tgUsername: String): F[Option[Invite]]

  def updateInfo(tgUserId: Long, info: Expert.Info): F[Unit]
}

object ExpertService {
  def apply[F[_]: Sync](implicit
      invitationsDao: InvitationsDao[F],
      userDao: UserDao[F],
      expertDao: ExpertDao[F]
  ): ExpertService[F] =
    new ExpertService[F] {
      override def inviteExpert(tgUsername: String, validFor: FiniteDuration): F[Invite] =
        Sync[F].delay(Instant.now).flatMap { time =>
          val invite = core.Invite(
            tgUsername = tgUsername,
            role = Role.Expert,
            validUntil = time.plusSeconds(validFor.toSeconds)
          )
          invitationsDao.upsertInvite(invite).as(invite)
        }

      override def getInvite(tgUsername: String): F[Option[Invite]] =
        invitationsDao.getInvite(tgUsername, Role.Expert)

      override def register(tgUserId: Long): F[Unit] =
        userDao.upsertUser(UserSource(tgUserId, Role.Expert))

      private def updateExpert(
          user: User,
          newInfo: Expert.Info
      ): F[Unit] =
        for {
          expert <- expertDao.getExpert(user)
          info = expert.map(_.info)
          updatedInfo = Expert.Info(
            name = newInfo.name.orElse(info.flatMap(_.name)),
            description = newInfo.description.orElse(info.flatMap(_.description)),
            photo = newInfo.photo.orElse(info.flatMap(_.photo))
          )
          updatedExpert = expert match {
            case Some(value) => value.copy(info = updatedInfo)
            case None =>
              Expert(
                user = user,
                info = updatedInfo,
                status = Expert.Status.New
              )
          }
          _ <- expertDao.upsertExpert(updatedExpert)
        } yield ()

      override def updateInfo(tgUserId: Long, newInfo: Expert.Info): F[Unit] = {
        for {
          optUser <- userDao.getUser(Role.Expert, tgUserId)
          _ <- optUser match {
            case Some(user) =>
              updateExpert(user, newInfo)
            case None =>
              new NoSuchElementException(s"User with tgId $tgUserId not found").raiseError[F, Unit]
          }
        } yield ()
      }
    }
}
