package io.iohk.atala.prism.connector

import com.typesafe.config.Config
import io.iohk.atala.prism.kotlin.identity.DID

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

object DidWhitelistLoader {
  def load(globalConfig: Config): Set[DID] = {
    val whitelistDids = globalConfig.getConfig("managementConsole").getList("whitelistDids").iterator().asScala

    whitelistDids.map { whitelistDid =>
      whitelistDid.unwrapped() match {
        case did: String =>
          Try(DID.fromString(did)).toOption match {
            case Some(value) =>
              value
            case None =>
              throw new IllegalArgumentException(s"Invalid DID in whitelist: $did")
          }
      }
    }.toSet
  }
}
