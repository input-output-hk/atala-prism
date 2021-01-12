package io.iohk.atala.prism.models

import enumeratum.{DoobieEnum, Enum, EnumEntry}

sealed abstract class ConnectionState(value: String) extends EnumEntry {
  override def entryName: String = value
}
object ConnectionState extends Enum[ConnectionState] with DoobieEnum[ConnectionState] {
  lazy val values = findValues

  final case object Invited extends ConnectionState("INVITED")
  final case object Connected extends ConnectionState("CONNECTED")
  final case object Revoked extends ConnectionState("REVOKED")
}
