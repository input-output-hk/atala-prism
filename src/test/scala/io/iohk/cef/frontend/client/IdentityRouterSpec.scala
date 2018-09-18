package io.iohk.cef.frontend.client

import java.util.Base64

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.DefaultJsonFormats
import io.iohk.cef.frontend.client.TransactionClient.IdentityTransactionRequest
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class IdentityRouterSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with DefaultJsonFormats {

  val nodeCore = mock[NodeCore[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]]

  when(nodeCore.receiveTransaction(any())).thenReturn(Future.successful(Right(())))
  val identityTransactionClientActor: ActorRef = system.actorOf(Props(new IdentityTransactionClientActor(nodeCore)))
  implicit val executionContext = system.dispatcher

  val identityService = new IdentityServiceApi(identityTransactionClientActor)
  lazy val routes = identityService.createIdentity

  "IdentityRouter" should {
    val encoder = Base64.getEncoder
    val pair = generateSigningKeyPair()
    val publicKeyString = Base64.getEncoder.encodeToString(pair.public.toByteString.toArray)

    "be able to create identity claim transaction (POST /transaction/identity)" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)
      val identity = IdentityTransactionRequest(
        "claim",
        "id",
        publicKeyString,
        encoder.encodeToString(signature.toByteString.toArray),
        1)

      val reqEntity = Marshal[IdentityTransactionRequest](identity).to[MessageEntity].futureValue

      val request = Post("/transaction/identity").withEntity(reqEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

      }
    }

    "be able to create identity link transaction (POST /transaction/identity)" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)

      val identity = IdentityTransactionRequest(
        "link",
        "id",
        publicKeyString,
        encoder.encodeToString(signature.toByteString.toArray),
        1)

      val reqEntity = Marshal[IdentityTransactionRequest](identity).to[MessageEntity].futureValue

      val request = Post("/transaction/identity").withEntity(reqEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

      }
    }

    "be able to create identity unlink transaction (POST /transaction/identity)" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)

      val identity = IdentityTransactionRequest(
        "unlink",
        "id",
        publicKeyString,
        encoder.encodeToString(signature.toByteString.toArray),
        1)
      val reqEntity = Marshal[IdentityTransactionRequest](identity).to[MessageEntity].futureValue

      val request = Post("/transaction/identity").withEntity(reqEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

      }
    }

    "fail transaction with bad request for invalid request  (POST /transaction/identity)" in {

      val identity = IdentityTransactionRequest("bad data", "id", publicKeyString, "signature", 1)
      val reqEntity = Marshal[IdentityTransactionRequest](identity).to[MessageEntity].futureValue

      val request = Post("/transaction/identity").withEntity(reqEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    }
  }
}
