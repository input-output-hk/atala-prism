package io.iohk.atala.mirror.config

import com.typesafe.config.Config

case class MirrorConfig(port: Int)

object MirrorConfig {

  def apply(globalConfig: Config): MirrorConfig = {
    val port = globalConfig.getInt("grpc.port")

    MirrorConfig(
      port = port
    )
  }

}
