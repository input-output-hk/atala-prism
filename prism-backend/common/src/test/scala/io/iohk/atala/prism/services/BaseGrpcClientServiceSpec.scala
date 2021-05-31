package io.iohk.atala.prism.services

import scala.concurrent.Future
import scala.concurrent.duration._
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scalapb.descriptors.{FieldDescriptor, PValue}
import com.google.protobuf.CodedOutputStream
import io.grpc.{CallOptions, Channel, Metadata}
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.stub.AbstractStub
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, ECPrivateKey}
import io.iohk.atala.prism.connector.{RequestAuthenticator, RequestNonce, SignedConnectorRequest}
import io.iohk.atala.prism.services.BaseGrpcClientService.AuthHeaders
import io.iohk.atala.prism.identity.DID
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *services.BaseGrpcClientServiceSpec"
class BaseGrpcClientServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "BaseGrpcClientService" should {
    "create metadata headers" in new GrpcClientStubs {
      val key1 = Metadata.Key.of("key1", Metadata.ASCII_STRING_MARSHALLER)
      val key2 = Metadata.Key.of("key2", Metadata.ASCII_STRING_MARSHALLER)

      val metadata = new Metadata
      metadata.put(key1, "value1")
      metadata.put(key2, "value2")

      client
        .createMetadataHeaders(key1 -> "value1", key2 -> "value2")
        .toString mustEqual metadata.toString
    }

    "sign request with did's private key" in new GrpcClientStubs {
      val metadata = new Metadata
      metadata.put(AuthHeaders.DID, DID.buildPrismDID("test").value)
      metadata.put(AuthHeaders.DID_KEY_ID, "didKeyId")
      metadata.put(AuthHeaders.DID_SIGNATURE, "c2lnbmF0dXJl")
      metadata.put(AuthHeaders.REQUEST_NONCE, "bm9uY2U=")

      client.signRequest(Request).toString mustBe metadata.toString
    }

    "call gRPC and return task" in new GrpcClientStubs {
      client.authenticatedCall(Request, _.testMethod).runSyncUnsafe(1.minute) mustBe Response("Request")
    }
  }

  trait GrpcClientStubs {
    val authConfig = BaseGrpcClientService.DidBasedAuthConfig(
      did = DID.buildPrismDID("test"),
      didKeyId = "didKeyId",
      didKeyPair = mock[ECKeyPair]
    )

    val requestAuthenticator = new RequestAuthenticator(EC) {
      override def signConnectorRequest(
          request: Array[Byte],
          privateKey: ECPrivateKey,
          requestNonce: RequestNonce
      ): SignedConnectorRequest = {
        SignedConnectorRequest(
          signature = Array(115, 105, 103, 110, 97, 116, 117, 114, 101), // bytes: signature
          requestNonce = Array(110, 111, 110, 99, 101) // bytes: nonce
        )
      }
    }

    case object Request extends GeneratedMessage {
      override def writeTo(output: CodedOutputStream): Unit = ()
      override def getFieldByNumber(fieldNumber: Int): Any = ???
      override def getField(field: FieldDescriptor): PValue = ???
      override def companion: GeneratedMessageCompanion[_] = ???
      override def serializedSize: Int = 0
      override def toProtoString: String = ???
    }

    case class Response(value: String)

    class GrpcServiceStub(channel: Channel) extends AbstractStub[GrpcServiceStub](channel, CallOptions.DEFAULT) {
      override protected def build(channel: Channel, callOptions: CallOptions): GrpcServiceStub =
        new GrpcServiceStub(channel)
      def testMethod(request: Request.type): Future[Response] = Future.successful(Response(request.toString))
    }

    val channel = InProcessChannelBuilder
      .forName("channel-name")
      .directExecutor()
      .build()

    class ClientService extends BaseGrpcClientService(new GrpcServiceStub(channel), requestAuthenticator, authConfig)

    val client = new ClientService
  }
}
