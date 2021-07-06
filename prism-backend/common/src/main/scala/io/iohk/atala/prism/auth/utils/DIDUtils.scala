package io.iohk.atala.prism.auth.utils

import io.iohk.atala.prism.auth.errors._
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.DIDFormat
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{CreateDIDOperation, DIDData}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

object DIDUtils {

  def validateDid(did: DID): FutureEither[AuthError, DIDData] = {
    did.getFormat match {
      case longFormDid: DIDFormat.LongForm =>
        longFormDid.validate match {
          case Left(DIDFormat.CanonicalSuffixMatchStateError) =>
            Future.successful(Left(CanonicalSuffixMatchStateError)).toFutureEither
          case Left(DIDFormat.InvalidAtalaOperationError) =>
            Future.successful(Left(InvalidAtalaOperationError)).toFutureEither
          case Right(validatedLongForm) =>
            validatedLongForm.initialState.operation.createDid match {
              case Some(CreateDIDOperation(Some(didData), _)) =>
                Future.successful(Right(didData)).toFutureEither
              case _ =>
                Future.successful(Left(NoCreateDidOperationError)).toFutureEither
            }
        }
      case _ => throw new IllegalStateException("Unreachable state")
    }
  }

  private def verifyPublicKey(curve: String, publicKey: ECPublicKey): Option[ECPublicKey] =
    Option.when(ECConfig.CURVE_NAME == curve && EC.isSecp256k1(publicKey.getCurvePoint))(publicKey)

  def findPublicKey(didData: node_models.DIDData, keyId: String)(implicit
      ec: ExecutionContext
  ): FutureEither[AuthError, ECPublicKey] = {
    Future {
      // TODO: Validate keyUsage and revocation
      // we haven't defined which keys can sign requests, and the model doesn't specify when a key is revoked
      val publicKeyOpt = didData.publicKeys
        .find(_.id == keyId)
        .flatMap(_.keyData.ecKeyData)
        .flatMap { data =>
          val pubk = EC.toPublicKey(x = data.x.toByteArray, y = data.y.toByteArray)
          verifyPublicKey(data.curve, pubk)
        }

      publicKeyOpt.toRight(UnknownPublicKeyId())
    }.toFutureEither
  }

}
