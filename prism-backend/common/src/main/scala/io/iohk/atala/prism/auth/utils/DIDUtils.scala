package io.iohk.atala.prism.auth.utils

import io.iohk.atala.prism.auth.errors._
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.LongFormPrismDid
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.protos.AtalaOperation.Operation.CreateDid
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}
import io.iohk.atala.prism.interop.toScalaProtos._
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.{CompressedEcKeyData, EcKeyData, Empty}

object DIDUtils {

  def validateDid(did: DID): FutureEither[AuthError, DIDData] = {
    did match {
      case longFormDid: LongFormPrismDid =>
        longFormDid.getInitialState.getOperation match {
          case crd: CreateDid =>
            Future.successful(Right(crd.getValue.getDidData.asScala)).toFutureEither
          case _ =>
            Future.successful(Left(NoCreateDidOperationError)).toFutureEither
        }
      case _ => throw new IllegalStateException("Unreachable state")
    }
  }

  private def verifyPublicKey(curve: String, publicKey: ECPublicKey): Option[ECPublicKey] =
    Option.when(ECConfig.getCURVE_NAME == curve && EC.isSecp256k1(publicKey.getCurvePoint))(publicKey)

  def findPublicKey(didData: node_models.DIDData, keyId: String)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ECPublicKey] = {
    Future {
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      val publicKeyOpt = didData.publicKeys
        .find(_.id == keyId)
        .map(_.keyData)
        .flatMap {
          case EcKeyData(data) =>
            // FIXME: remove that if statement after fixing whitelist DID's (they should use really uncompressed keys or really compressed ones)
            if (data.x.size() > 32) verifyPublicKey(data.curve, EC.toPublicKeyFromCompressed(data.x.toByteArray))
            else
              verifyPublicKey(
                data.curve,
                EC.toPublicKeyFromByteCoordinates(data.x.toByteArray, data.y.toByteArray)
              )
          case CompressedEcKeyData(data) =>
            verifyPublicKey(data.curve, EC.toPublicKeyFromCompressed(data.data.toByteArray))
          case Empty => None
        }

      publicKeyOpt.toRight(UnknownPublicKeyId())
    }.toFutureEither
  }

}
