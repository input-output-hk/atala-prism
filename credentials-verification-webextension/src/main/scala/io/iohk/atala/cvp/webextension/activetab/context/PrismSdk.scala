package io.iohk.atala.cvp.webextension.activetab.context

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.activetab.isolated.ExtensionAPI
import io.iohk.atala.cvp.webextension.activetab.models.{JsSignedMessage, JsUserDetails}
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.PropertyDescriptor

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
          "getWalletStatus" -> js.Any.fromFunction0(getWalletStatus),
          "login" -> js.Any.fromFunction0(login),
          "requestSignature" -> js.Any.fromFunction2(requestSignature),
          "signConnectorRequest" -> js.Any.fromFunction2(signConnectorRequest),
          "verifySignedCredential" -> js.Any.fromFunction2(verifySignedCredential)
        ): js.UndefOr[js.Any]
      }
    )
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

  def signConnectorRequest(sessionId: String, request: js.Array[Byte]): js.Promise[JsSignedMessage] = {
    extensionAPI
      .signConnectorRequest(sessionId, ConnectorRequest(request.toArray))
      .map(_.signedMessage)
      .map(sm => JsSignedMessage(sm.did, sm.didKeyId, sm.signature.toJSArray))
      .toJSPromise
  }

  def verifySignedCredential(sessionId: String, signedCredentialStringRepresentation: String): js.Promise[Boolean] = {
    extensionAPI
      .verifySignedCredential(sessionId, signedCredentialStringRepresentation)
      .map(_.result)
      .toJSPromise
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
