package io.iohk.atala.cvp.webextension.activetab.context

import cats.data.ValidatedNel
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.activetab.isolated.ExtensionAPI
import io.iohk.atala.cvp.webextension.activetab.models.{
  JsRequestApprovalResult,
  JsSdkDetails,
  JsSignedMessage,
  JsUserDetails
}
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject, PendingRequest}
import io.iohk.atala.cvp.webextension.util.ByteOps
import io.iohk.atala.cvp.webextension.util.NullableOps._
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials._
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.SHA256DigestCompanion

import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|._
import scala.scalajs.js.PropertyDescriptor
import scala.util.Try

/**
  * This is the sdk object that can be accessed from any website to interact with our extension.
  *
  * There are important details to take care on the exposed functions:
  * - They should be JavaScript friendly instead of Scala, for example, use Promise instead of Future.
  * - They shouldn't expose the internal messaging representation.
  * - The Scala functions defined (like getWalletStatus) should be public, otherwise, the compiler fails.
  */
class PrismSdk(name: String = "prism", extensionAPI: ExtensionAPI)(implicit
    ec: ExecutionContext
) {

  def inject(parent: js.Object): Unit = {
    js.Object.defineProperty(
      parent,
      name,
      new PropertyDescriptor {
        enumerable = false
        writable = false
        configurable = false
        // NOTE: The highlighted error is a bug on IntelliJ as the code compiles properly
        value = js.Dictionary(
          "getSdkDetails" -> js.Any.fromFunction0(() => getSdkDetails()),
          "getWalletStatus" -> js.Any.fromFunction0(() => getWalletStatus()),
          "login" -> js.Any.fromFunction0(() => login()),
          "requestSignature" -> js.Any.fromFunction2(requestCredentialIssuanceApproval),
          "signConnectorRequest" -> js.Any.fromFunction3(signConnectorRequest),
          "verifySignedCredential" -> js.Any.fromFunction3(verifySignedCredential),
          "revokeCredential" -> js.Any.fromFunction5(requestCredentialRevocationApproval)
        ): js.UndefOr[js.Any]
      }
    )
  }

  def getSdkDetails(): js.Promise[JsSdkDetails] = {
    extensionAPI
      .getSdkDetails()
      .map(e => JsSdkDetails(extensionId = e.extensionId))
      .toJSPromise
  }

  def getWalletStatus(): js.Promise[String] = {
    extensionAPI.getWalletStatus().map(_.status).toJSPromise
  }

  def login(): js.Promise[JsUserDetails] = {
    extensionAPI
      .login()
      .map(_.userDetails)
      .map(ud => JsUserDetails(ud.sessionId, ud.name, ud.role, ud.logo.toJSArray))
      .toJSPromise
  }

  // used to request signing a credential, which needs to be approved manually in the UI
  def requestCredentialIssuanceApproval(
      sessionId: String,
      payloadAsJson: String
  ): js.Promise[JsRequestApprovalResult] = {
    val maybeSubject = Try(readJsonAs[CredentialSubject](payloadAsJson))

    val result = for {
      request <- Future.fromTry(maybeSubject.map(PendingRequest.IssueCredential))
      approvalResult <-
        extensionAPI.enqueueRequestRequiringManualApproval(sessionId, request).map(JsRequestApprovalResult)
    } yield approvalResult

    result.toJSPromise
  }

  // TODO: Return the transaction id after publishing the operation
  /**
    * Enqueues a request to revoke the given credential
    *
    * @param sessionId the session that's being used to enqueue the operation
    * @param signedCredentialStringRepresentation the signed credential in its canonical form
    * @param batchIdStr the batch id returned when issuing the credential, must be a hex-encoded string
    * @param batchOperationHashStr the hash of the operation batch that issued the credential,
    *                              must be a hex-encoded string, or a base64 encoded string.
    * @param credentialIdStr the console internal credential Id, must be an UUID
    * @return a promise that's resolved once the operation is approved/rejected
    */
  def requestCredentialRevocationApproval(
      sessionId: String,
      signedCredentialStringRepresentation: String,
      batchIdStr: String,
      batchOperationHashStr: String,
      credentialIdStr: String
  ): js.Promise[JsRequestApprovalResult] = {
    val requestT = for {
      batchId <- Try {
        CredentialBatchIdCompanion
          .fromString(batchIdStr)
          .getNullable(throw new RuntimeException("Invalid batch id"))
      }
      batchOperationHash <- Try { SHA256DigestCompanion.fromHex(batchOperationHashStr) }
        .orElse {
          // For some reason the frontend gets a base64 encoded string for the operation hash
          // to simplify their work, we just try to parse the hex, falling back to the base64 version
          // node that the url decoder version is not used on purpose.
          Try(Base64.getDecoder.decode(batchOperationHashStr))
            .map(ByteOps.convertBytesToHex)
            .map(SHA256DigestCompanion.fromHex)
        }
      credentialId <- Try {
        UUID.fromString(credentialIdStr)
      }
    } yield PendingRequest.RevokeCredential(
      signedCredentialStringRepresentation = signedCredentialStringRepresentation,
      batchId = batchId,
      batchOperationHash = batchOperationHash,
      credentialId = credentialId
    )

    val result = for {
      request <- Future.fromTry(requestT)
      transactionId <- extensionAPI.enqueueRequestRequiringManualApproval(sessionId, request)
      convertedResponse = JsRequestApprovalResult(transactionId)
    } yield convertedResponse

    result.toJSPromise
  }

  def signConnectorRequest(
      sessionId: String,
      requestJS: js.Array[Double],
      nonceJS: js.Array[Double]
  ): js.Promise[JsSignedMessage] = {
    val requestBytes = toByteArray(requestJS)
    val optionalNonceBytes = if (js.isUndefined(nonceJS)) Future.successful(None) else toByteArray(nonceJS).map(Some(_))
    val resultFuture = for {
      request <- requestBytes
      nonce <- optionalNonceBytes
      signedMessage <- signMessageFuture(sessionId, request, nonce)
    } yield signedMessage
    resultFuture.toJSPromise
  }

  def verifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String,
      encodedMerkleProof: String
  ): js.Promise[js.Array[String]] = {
    extensionAPI
      .verifySignedCredential(sessionId, signedCredentialStringRepresentation, encodedMerkleProof)
      .map(event => toJsErrorList(event.result))
      .toJSPromise
  }

  private def signMessageFuture(
      sessionId: String,
      requestBytes: Array[Byte],
      nonceBytes: Option[Array[Byte]]
  ) = {
    extensionAPI
      .signConnectorRequest(sessionId, ConnectorRequest(requestBytes), nonceBytes)
      .map(_.signedMessage)
      .map { sm =>
        JsSignedMessage(
          did = sm.did.value,
          didKeyId = sm.didKeyId,
          encodedSignature = sm.base64UrlSignature,
          encodedNonce = sm.base64UrlNonce
        )
      }
  }

  private def toByteArray(jsBytes: js.Array[Double]): Future[Array[Byte]] = {
    val scalaBytes = jsBytes.toArray.flatMap(x => Try(x.toByte).toOption)
    if (scalaBytes.length != jsBytes.length) {
      Future
        .failed(
          new RuntimeException(
            "The request should only contain bytes [0, 255], some of the values aren't bytes"
          )
        )
    } else {
      Future.successful(scalaBytes)
    }
  }

  private def toJsErrorList(validations: ValidatedNel[VerificationException, Unit]): js.Array[String] = {
    def toErrorCode(err: VerificationException): String =
      err match {
        case credentialWasRevoked: CredentialWasRevoked =>
          s"The credential was revoked on ${credentialWasRevoked.revokedOn}"
        case batchWasRevoked: BatchWasRevoked =>
          s"The batch was revoked on ${batchWasRevoked.revokedOn}"
        case InvalidMerkleProof =>
          "Invalid Merkle proof"
        case keyWasNotValid: KeyWasNotValid =>
          s"""The signing key was added after the credential was issued.
             |Key added on: ${keyWasNotValid.keyAddedOn}
             |Credential issued on: ${keyWasNotValid.credentialIssuedOn}
             |""".stripMargin
        case keyWasRevoked: KeyWasRevoked =>
          s"""The signing key was revoked before the credential was issued.
             |Credential issued on: ${keyWasRevoked.credentialIssuedOn}
             |Key revoked on: ${keyWasRevoked.keyRevokedOn}
             |""".stripMargin
        case InvalidSignature =>
          "The credential signature was invalid"
        case _ =>
          "Unknown verification error"
      }

    validations.fold(
      errors => js.Array[String]((errors.toList map toErrorCode): _*),
      _ => js.Array[String]()
    )
  }

  private def readJsonAs[T](jsonAsString: String)(implicit decoder: io.circe.Decoder[T]): T = {
    val exampleJson = """{
                        |  "id": "credentialId is a UUID",
                        |  "properties": {
                        |    "key1": "Example University",
                        |    "key2": "en"
                        |  }
                        |}""".stripMargin
    parse(jsonAsString)
      .getOrElse(Json.obj())
      .as[T]
      .getOrElse(throw new RuntimeException(s"Type could not be loaded from JSON expected:\n $exampleJson"))
  }
}
