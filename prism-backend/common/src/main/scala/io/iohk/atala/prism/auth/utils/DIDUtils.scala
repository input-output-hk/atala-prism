package io.iohk.atala.prism.auth.utils

import io.iohk.atala.prism.auth.errors._
import io.iohk.atala.prism.kotlin.crypto.{EC}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.kotlin.crypto.keys.{ECPublicKey}
import io.iohk.atala.prism.kotlin.identity.DIDFormatException.{
  CanonicalSuffixMatchStateException,
  InvalidAtalaOperationException
}
import io.iohk.atala.prism.kotlin.identity.{DID, LongForm}
import io.iohk.atala.prism.kotlin.protos.AtalaOperation.Operation.CreateDid
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import io.iohk.atala.prism.interop.toScalaSDK._

object DIDUtils {

  def validateDid(did: DID): FutureEither[AuthError, DIDData] = {
    did.getFormat match {
      case longFormDid: LongForm =>
        Try(longFormDid.validate) match {
          case Failure(err) =>
            err match {
              case _: InvalidAtalaOperationException =>
                Future.successful(Left(InvalidAtalaOperationError)).toFutureEither
              case _: CanonicalSuffixMatchStateException =>
                Future.successful(Left(CanonicalSuffixMatchStateError)).toFutureEither
            }
          case Success(validatedLongForm) =>
            validatedLongForm.getInitialState.getOperation match {
              case crd: CreateDid =>
                Future.successful(Right(crd.getValue.getDidData.asScala)).toFutureEither
              case _ =>
                Future.successful(Left(NoCreateDidOperationError)).toFutureEither
            }
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
        .flatMap(_.keyData.ecKeyData)
        .flatMap { data =>
          val pubk = EC.toPublicKey(data.x.toByteArray, data.y.toByteArray)
          verifyPublicKey(data.curve, pubk)
        }

      publicKeyOpt.toRight(UnknownPublicKeyId())
    }.toFutureEither
  }

}
