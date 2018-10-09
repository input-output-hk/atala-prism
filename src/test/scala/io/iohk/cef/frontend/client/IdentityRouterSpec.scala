package io.iohk.cef.frontend.client

import java.util.Base64

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.iohk.cef.core.NodeCore
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.models.CreateIdentityTransactionRequest
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class IdentityRouterSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

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
      val entity = CreateIdentityTransactionRequest(
        `type` = IdentityTransactionType.Claim,
        identity = "id",
        ledgerId = 1,
        publicKey = pair.public,
        privateKey = pair.`private`)

      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities", json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    "be able to create identity link transaction" in {
      val entity = CreateIdentityTransactionRequest(
        `type` = IdentityTransactionType.Link,
        identity = "id",
        ledgerId = 1,
        publicKey = pair.public,
        privateKey = pair.`private`)

      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities", json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    "be able to create identity unlink transaction" in {
      val entity = CreateIdentityTransactionRequest(
        `type` = IdentityTransactionType.Unlink,
        identity = "id",
        ledgerId = 1,
        publicKey = pair.public,
        privateKey = pair.`private`)

      val json = Marshal(entity).to[MessageEntity].futureValue
      val request = Post("/identities", json)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    // TODO: Add negative tests
  }
}
