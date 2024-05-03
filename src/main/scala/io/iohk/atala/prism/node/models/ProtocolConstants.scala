package io.iohk.atala.prism.node.models

import com.typesafe.config.{Config, ConfigFactory}
import util.Try

object ProtocolConstants {

  private val globalConfig: Config = ConfigFactory.load()

  private val defaultServiceLimit = 50
  private val defaultPublicKeysLimit = 50
  private val defaultServiceEndpointCharLength = 300
  private val defaultServiceTypeCharLength = 100
  private val defaultContextStringCharLength = 100
  private val defaultIdCharLimit = 50

  val servicesLimit: Int = Try(globalConfig.getInt("didServicesLimit")).toOption.getOrElse(defaultServiceLimit)
  val publicKeysLimit: Int = Try(globalConfig.getInt("didPublicKeysLimit")).toOption.getOrElse(defaultPublicKeysLimit)
  val serviceEndpointCharLenLimit: Int =
    Try(globalConfig.getInt("didServiceEndpointCharLimit")).toOption.getOrElse(defaultServiceEndpointCharLength)
  val serviceTypeCharLimit: Int =
    Try(globalConfig.getInt("didServiceTypeCharLimit")).toOption.getOrElse(defaultServiceTypeCharLength)
  val idCharLenLimit: Int = Try(globalConfig.getInt("didIdCharLenLimit")).toOption.getOrElse(defaultIdCharLimit)
  val contextStringCharLimit: Int =
    Try(globalConfig.getInt("contextStringCharLimit")).toOption.getOrElse(defaultContextStringCharLength)

  val secpCurveName = "secp256k1"
  val ed25519CurveName = "Ed25519"
  val x25519CurveName = "X25519"

  val supportedEllipticCurves: Seq[String] = List(secpCurveName, ed25519CurveName, x25519CurveName)

}
