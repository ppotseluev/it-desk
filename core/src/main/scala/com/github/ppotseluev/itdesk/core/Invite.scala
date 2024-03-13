package com.github.ppotseluev.itdesk.core

import com.github.ppotseluev.itdesk.core.user.Role
import java.time.Instant

case class Invite(
    tgUsername: String,
    role: Role,
    validUntil: Instant
)
