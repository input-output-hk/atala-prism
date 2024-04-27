package io.iohk.atala.prism.node.auth.utils

import com.typesafe.config.Config
import io.iohk.atala.prism.node.identity.{PrismDid => DID}

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

object DidWhitelistLoader {
  def load(globalConfig: Config, paths: String*): Set[DID] = {
    val whitelistDids = paths
      .foldLeft(globalConfig)(_.getConfig(_))
      .getList("whitelistDids")
      .iterator()
      .asScala

    whitelistDids.map { whitelistDid =>
      whitelistDid.unwrapped() match {
        case did: String =>
          Try(DID.fromString(did)).toOption match {
            case Some(value) =>
              value
            case None =>
              throw new IllegalArgumentException(
                s"Invalid DID in whitelist: $did"
              )
          }
        case other =>
          throw new IllegalArgumentException(
            "Expected whitelisted DID as String, but got " + other.getClass
          )
      }
    }.toSet
  }
}
