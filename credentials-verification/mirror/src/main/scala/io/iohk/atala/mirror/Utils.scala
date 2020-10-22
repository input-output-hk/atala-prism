package io.iohk.atala.mirror

import java.util.UUID

import scala.util.Try

object Utils {

  def parseUUID(uuid: String): Option[UUID] = Try(UUID.fromString(uuid)).toOption

}
