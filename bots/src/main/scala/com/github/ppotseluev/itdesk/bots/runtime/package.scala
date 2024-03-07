package com.github.ppotseluev.itdesk.bots

import cats.~>
import com.github.ppotseluev.itdesk.bots.core.BotDsl
import com.github.ppotseluev.itdesk.bots.core.BotId
import com.github.ppotseluev.itdesk.bots.core.ChatId
import com.github.ppotseluev.itdesk.storage.KeyValueDao

package object runtime {
  type BotStateDao[F[_]] = KeyValueDao[F, (ChatId, BotId), BotInfo]
  type BotInterpreter[F[_]] = BotDsl ~> F
}
