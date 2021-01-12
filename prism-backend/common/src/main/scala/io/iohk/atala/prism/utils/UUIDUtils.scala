package io.iohk.atala.prism.utils

import java.util.UUID

import scala.util.Try

object UUIDUtils {

  def parseUUID(uuid: String): Option[UUID] = Try(UUID.fromString(uuid)).toOption

}
