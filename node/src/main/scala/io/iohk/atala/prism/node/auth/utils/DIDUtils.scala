package io.iohk.atala.prism.node.auth.utils

import io.iohk.atala.prism.node.auth.errors._
import io.iohk.atala.prism.identity.{LongFormPrismDid, PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.node.interop.toScalaProtos._
import io.iohk.atala.prism.node.utils.FutureEither
import io.iohk.atala.prism.protos.AtalaOperation.Operation.CreateDid
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.{CompressedEcKeyData, EcKeyData, Empty}
import io.iohk.atala.prism.node.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

object DIDUtils {

  def validateDid(did: DID): FutureEither[AuthError, DIDData] = {
    did match {
      case longFormDid: LongFormPrismDid =>
        longFormDid.getInitialState.getOperation match {
          case crd: CreateDid => {
            val creationData = crd.getValue.getDidData.asScala
            Future
              .successful(
                Right(
                  DIDData(longFormDid.getDid.toString, creationData.publicKeys)
                )
              )
              .toFutureEither
          }
          case _ =>
            Future.successful(Left(NoCreateDidOperationError)).toFutureEither
        }
      case _ => throw new IllegalStateException("Unreachable state")
    }
  }

  def validateDidEith(did: DID): Either[AuthError, DIDData] = {
    did match {
      case longFormDid: LongFormPrismDid =>
        longFormDid.getInitialState.getOperation match {
          case crd: CreateDid =>
            val creationData = crd.getValue.getDidData.asScala
            Right(DIDData(longFormDid.getDid.toString, creationData.publicKeys))
          case _ =>
            Left(NoCreateDidOperationError)
        }
      case _ => Left(InvalidRequest("validateDidEith: Unreachable state"))
    }
  }

  private def verifyPublicKey(
      curve: String,
      publicKey: SecpPublicKey
  ): Option[SecpPublicKey] =
    Option.when(
      publicKey.curveName == curve && CryptoUtils.isSecp256k1(publicKey)
    )(publicKey)

  def findPublicKey(didData: node_models.DIDData, keyId: String)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, SecpPublicKey] = {
    Future {
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      val publicKeyOpt = didData.publicKeys
        .find(_.id == keyId)
        .map(_.keyData)
        .flatMap {
          case EcKeyData(data) =>
            // FIXME: remove that if statement after fixing whitelist DID's (they should use really uncompressed keys or really compressed ones)
            if (data.x.size() > 32)
              verifyPublicKey(
                data.curve,
                SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(data.x.toByteArray.toVector)
              )
            else
              verifyPublicKey(
                data.curve,
                SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(
                  data.x.toByteArray,
                  data.y.toByteArray
                )
              )
          case CompressedEcKeyData(data) =>
            verifyPublicKey(
              data.curve,
              SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(data.data.toByteArray.toVector)
            )
          case Empty => None
        }

      publicKeyOpt.toRight(UnknownPublicKeyId())
    }.toFutureEither
  }

  def findPublicKeyEith(didData: node_models.DIDData, keyId: String): Either[AuthError, SecpPublicKey] = {

    // TODO: Validate keyUsage and revocation
    // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
    val publicKeyOpt = didData.publicKeys
      .find(_.id == keyId)
      .map(_.keyData)
      .flatMap {
        case EcKeyData(data) =>
          // FIXME: remove that if statement after fixing whitelist DID's (they should use really uncompressed keys or really compressed ones)
          if (data.x.size() > 32)
            verifyPublicKey(
              data.curve,
              SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(data.x.toByteArray.toVector)
            )
          else
            verifyPublicKey(
              data.curve,
              SecpPublicKey.unsafeToSecpPublicKeyFromByteCoordinates(
                data.x.toByteArray,
                data.y.toByteArray
              )
            )
        case CompressedEcKeyData(data) =>
          verifyPublicKey(
            data.curve,
            SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(data.data.toByteArray.toVector)
          )
        case Empty => None
      }

    publicKeyOpt.toRight(UnknownPublicKeyId())
  }

}
