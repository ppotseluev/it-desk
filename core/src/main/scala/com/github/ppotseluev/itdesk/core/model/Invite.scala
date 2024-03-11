package com.github.ppotseluev.itdesk.core.model

import java.time.Instant

case class Invite(
    tgUsername: String,
    role: Role,
    validUntil: Instant
)
