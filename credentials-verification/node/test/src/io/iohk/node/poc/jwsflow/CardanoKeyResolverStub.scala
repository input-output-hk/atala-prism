package io.iohk.node.poc.jwsflow

import java.net.URI
import java.security.PublicKey

import io.iohk.cvp.utils.syntax._
import io.iohk.node.grpc.ProtoCodecs
import io.iohk.prism.protos.node_api
import io.iohk.prism.protos.node_api.GetDidDocumentRequest
import io.iohk.prism.protos.node_models.DIDData
import net.jtownson.odyssey.PublicKeyResolver
import org.scalatest.OptionValues._

import scala.concurrent.{ExecutionContext, Future}

// We define some classes to illustrate what happens in the different components
case class CardanoKeyResolverStub(nodeService: node_api.NodeServiceGrpc.NodeServiceBlockingStub)(implicit
    ec: ExecutionContext
) extends PublicKeyResolver {

  private def keyParts(publicKeyRef: URI): (String, String) = {
    val parts = publicKeyRef.toString.split(':')
    if (parts.length != 3) {
      throw new IllegalArgumentException(s"Expected did parts did:prism:<suffix>. Got ${parts.mkString(":")}.")
    } else {
      val suffixParts = publicKeyRef.toString.split('#')
      if (parts(0) != "did") {
        throw new IllegalArgumentException(s"Expected protocol 'did'. Got '${parts(0)}'.")
      } else if (parts(1) != "prism") {
        throw new IllegalArgumentException(s"Expected did method 'prism'. Got '${parts(1)}'.")
      } else if (suffixParts.length != 2) {
        throw new IllegalArgumentException(
          s"Expected suffix parts of the form <did>#<key id>. Got ${suffixParts.length} parts."
        )
      } else {
        (suffixParts(0), suffixParts(1))
      }
    }
  }

  private def lift[A, B](f: A => B): A => Future[B] = { a =>
    f(a).tryF
  }

  /**
    * Resolve public keys from Cardano using URLs of the form did:prism:<suffix>#<key id>
    *
    * @param publicKeyRef the URL of a public key in the form did:prism:<suffix>#<key id>
    * @return The public key if one is resolved. If the key does not exist, the Future will
    *         fail with an error.
    */
  override def resolvePublicKey(publicKeyRef: URI): Future[PublicKey] = {
    for {
      (did, keyId) <- lift(keyParts)(publicKeyRef)
      nodeResponse = nodeService.getDidDocument(GetDidDocumentRequest(did))
      didData <- nodeResponse.document.fold[Future[DIDData]](
        Future.failed(new Exception(s"No data found for did $did"))
      )(Future.successful)
      maybeKey = didData.publicKeys.find(publicKey => publicKey.id == keyId)
      pk <- maybeKey.fold[Future[PublicKey]](
        Future.failed(new Exception(s"No key found matching key id ${publicKeyRef.toString}"))
      )(nodeKey => Future.successful(ProtoCodecs.fromProtoKeyLegacy(nodeKey).value))
    } yield {
      pk
    }
  }
}
