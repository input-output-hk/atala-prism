package io.iohk.cef.builder

import java.security.SecureRandom

trait SecureRandomBuilder {
  lazy val secureRandom: SecureRandom =
    Config.secureRandomAlgo.map(SecureRandom.getInstance).getOrElse(new SecureRandom())
}

object SecureRandomBuilder extends SecureRandomBuilder
