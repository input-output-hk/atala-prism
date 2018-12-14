package io.iohk.cef.test.builder

import com.typesafe.config.ConfigFactory

object Config {

  val config = ConfigFactory.load

  val secureRandomAlgo: Option[String] =
    if (config.hasPath("secure-random-algo")) Some(config.getString("secure-random-algo"))
    else None

}
