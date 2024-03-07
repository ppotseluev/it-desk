package com.github.ppotseluev.itdesk.api

import enumeratum._

sealed abstract class BotType(val id: String) extends EnumEntry

object BotType extends Enum[BotType] {
  case object GreetingBot extends BotType("greeting")

  override val values: IndexedSeq[BotType] = findValues
}
