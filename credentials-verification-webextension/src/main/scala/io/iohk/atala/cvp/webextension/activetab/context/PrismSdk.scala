package io.iohk.atala.cvp.webextension.activetab.context

import cats.data.ValidatedNel
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.prism.credentials.VerificationError.{
  BatchWasRevoked,
  CredentialWasRevoked,
  InvalidMerkleProof,
  InvalidSignature,
  KeyWasNotValid,
  KeyWasRevoked
}
import io.iohk.atala.cvp.webextension.activetab.isolated.ExtensionAPI
import io.iohk.atala.cvp.webextension.activetab.models.{JsSdkDetails, JsSignedMessage, JsUserDetails}
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
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
          "requestSignature" -> js.Any.fromFunction2(requestSignature),
          "signConnectorRequest" -> js.Any.fromFunction2(signConnectorRequest),
          "verifySignedCredential" -> js.Any.fromFunction2(verifySignedCredential)
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

  def requestSignature(sessionId: String, payloadAsJson: String): Unit = {
    val subject = readJsonAs[CredentialSubject](payloadAsJson)
    extensionAPI.requestSignature(sessionId, subject)
  }

  def signConnectorRequest(sessionId: String, request: js.Array[Double]): js.Promise[JsSignedMessage] = {
    val scalaBytes = request.toArray.flatMap(x => Try(x.toByte).toOption)

    if (scalaBytes.length != request.length) {
      Future
        .failed(new RuntimeException("The request should only contain bytes [0, 255], some of the values aren't bytes"))
        .toJSPromise
    } else {
      extensionAPI
        .signConnectorRequest(sessionId, ConnectorRequest(scalaBytes))
        .map(_.signedMessage)
        .map { sm =>
          JsSignedMessage(
            did = sm.did.value,
            didKeyId = sm.didKeyId,
            encodedSignature = sm.base64UrlSignature,
            encodedNonce = sm.base64UrlNonce
          )
        }
        .toJSPromise
    }
  }

  def verifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String
  ): js.Promise[js.Array[String]] = {
    extensionAPI
      .verifySignedCredential(sessionId, signedCredentialStringRepresentation)
      .map(event => toJsErrorList(event.result))
      .toJSPromise
  }

  private def toJsErrorList(validations: ValidatedNel[VerificationError, Unit]): js.Array[String] = {
    def toErrorCode(err: VerificationError): String =
      err match {
        case CredentialWasRevoked(revokedOn) =>
          s"The credential was revoked on $revokedOn"
        case BatchWasRevoked(revokedOn) =>
          s"The batch was revoked on $revokedOn"
        case InvalidMerkleProof =>
          "Invalid Merkle proof"
        case KeyWasNotValid(keyAddedOn, credentialIssuedOn) =>
          s"The signing key was added after the credential was issued.\nKey added on: $keyAddedOn\nCredential issued on: $credentialIssuedOn"
        case KeyWasRevoked(credentialIssuedOn, keyRevokedOn) =>
          s"The signing key was revoked before the credential was issued.\nCredential issued on: $credentialIssuedOn\nKey revoked on: $keyRevokedOn"
        case InvalidSignature =>
          "The credential signature was invalid"
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
