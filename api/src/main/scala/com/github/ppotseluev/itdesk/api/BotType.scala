package com.github.ppotseluev.itdesk.api

import enumeratum._

sealed abstract class BotType(val id: String) extends EnumEntry

object BotType extends Enum[BotType] {
  case object AdminBot extends BotType("admin")
  case object ExpertBot extends BotType("expert")

  override val values: IndexedSeq[BotType] = findValues
}
