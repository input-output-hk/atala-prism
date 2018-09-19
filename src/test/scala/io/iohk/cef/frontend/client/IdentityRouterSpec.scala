package io.iohk.cef.frontend.client

import java.util.Base64

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.models.IdentityTransactionRequest
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
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
  implicit val executionContext = system.dispatcher

  val service = new IdentityTransactionService(nodeCore)
  val identityService = new IdentityServiceApi(service)
  lazy val routes = identityService.createIdentity

  "POST /identities" should {
    val encoder = Base64.getEncoder
    val pair = generateSigningKeyPair()

    "be able to create identity claim transaction" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)
      val entity = IdentityTransactionRequest(
        Claim("id", pair.public, signature),
        1)
      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities", json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    "be able to create identity link transaction" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)
      val entity = IdentityTransactionRequest(
        Link("id", pair.public, signature),
        1)
      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities").withEntity(json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    "be able to create identity unlink transaction" in {
      val signature = sign(ByteString("id") ++ pair.public.toByteString, pair.`private`)
      val entity = IdentityTransactionRequest(
        Unlink("id", pair.public, signature),
        1)
      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities").withEntity(json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    // TODO: Add negative tests
  }
}
