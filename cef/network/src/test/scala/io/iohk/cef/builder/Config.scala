package io.iohk.cef.builder

import com.typesafe.config.ConfigFactory

object Config {

  val config = ConfigFactory.load("application.test")

  val secureRandomAlgo: Option[String] =
    if(config.hasPath("secure-random-algo")) Some(config.getString("secure-random-algo"))
    else None

}
