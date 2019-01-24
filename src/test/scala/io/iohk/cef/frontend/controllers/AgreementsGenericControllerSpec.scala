package io.iohk.cef.frontend.controllers

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.agreements.{AgreementMessage, AgreementsService, UserId}
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.DataItem
import io.iohk.cef.ledger.chimeric.{ChimericTx, CreateCurrency}
import io.iohk.cef.network.MessageStream
import io.iohk.cef.test.DummyMessageStream
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar._
import play.api.libs.json.{Format, Json}

class AgreementsGenericControllerSpec extends WordSpec with ScalatestRouteTest with PlayJsonSupport {

  import AgreementsGenericControllerSpec._
  import io.iohk.cef.frontend.controllers.common.Codecs._

  implicit val executionContext = system.dispatcher

  def dummyService[T]: AgreementsService[T] = mock[AgreementsService[T]]

  val keys = generateSigningKeyPair()
  val controller = new AgreementsGenericController
  val certificateRoutes = controller.routes("certificates", dummyService[DataItem[Certificate]])
  val chimericRoutes = controller.routes("chimeric", dummyService[ChimericTx])

  "POST /agreements/certificates/propose" should {
    "propose an item" in {
      val certificate = Certificate("certificateId", "2019/Jan/01")
      val signature = sign(certificate, keys.`private`)
      val body =
        s"""
          |{
          |  "correlationId": "${UUID.randomUUID()}",
          |  "data": {
          |    "id": "itemId",
          |    "data": ${Json.toJson(certificate)},
          |    "witnesses": [],
          |    "owners": [
          |      {
          |        "key": "${keys.public.toCompactString()}",
          |        "signature": "${signature.toCompactString()}"
          |      }
          |    ]
          |  },
          |  "to": ["1111", "2222"]
          |}
        """.stripMargin

      val request = Post("/agreements/certificates/propose", HttpEntity(ContentTypes.`application/json`, body))

      request ~> certificateRoutes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }

  "POST /agreements/certificates/agree" should {
    "agree to an item" in {
      val certificate = Certificate("certificateId", "2019/Jan/01")
      val signature = sign(certificate, keys.`private`)
      val body =
        s"""
           |{
           |  "correlationId": "${UUID.randomUUID()}",
           |  "data": {
           |    "id": "itemId",
           |    "data": ${Json.toJson(certificate)},
           |    "witnesses": [],
           |    "owners": [
           |      {
           |        "key": "${keys.public.toCompactString()}",
           |        "signature": "${signature.toCompactString()}"
           |      }
           |    ]
           |  }
           |}
        """.stripMargin

      val request = Post("/agreements/certificates/agree", HttpEntity(ContentTypes.`application/json`, body))

      request ~> certificateRoutes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }

  "POST /agreements/whatever/agree" should {
    "reject an unknown correlation id" in {
      def dummyAgreementService: AgreementsService[String] = new AgreementsService[String] {
        override def propose(correlationId: String, data: String, to: List[UserId]): Unit = ???

        override def agree(correlationId: String, data: String): Unit = {
          throw new IllegalArgumentException("exception")
        }

        override def decline(correlationId: String): Unit = ???

        override val agreementEvents: MessageStream[AgreementMessage[String]] =
          new DummyMessageStream(Observable.empty)(TestScheduler())
      }
      val routes = controller.routes("generic", dummyAgreementService)
      val body =
        s"""
           |{
           |  "correlationId": "agreementId",
           |  "data": "test"
           |}
        """.stripMargin

      val request = Post("/agreements/generic/agree", HttpEntity(ContentTypes.`application/json`, body))

      request ~> routes ~> check {
        status must ===(StatusCodes.BadRequest)
      }
    }
  }

  "POST /agreements/chimeric/propose" should {
    "propose a chimeric transaction" in {
      val tx = ChimericTx(
        List(
          CreateCurrency("ADA")
        )
      )

      val body =
        s"""
           |{
           |  "correlationId": "${UUID.randomUUID()}",
           |  "data": ${Json.toJson(tx).toString()},
           |  "to": ["1111", "2222"]
           |}
        """.stripMargin

      val request = Post("/agreements/chimeric/propose", HttpEntity(ContentTypes.`application/json`, body))

      request ~> chimericRoutes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }

  "POST /agreements/whatever/whatever" should {
    val controller = new AgreementsGenericController
    val certificateRoutes = controller.routes("whatever", dummyService[String])
    "reject an empty correlationId when declining" in {
      val decline =
        s"""
           |{
           |  "correlationId": ""
           |}
        """.stripMargin

      val requestDecline =
        Post("/agreements/whatever/decline", HttpEntity(ContentTypes.`application/json`, decline))

      requestDecline ~> certificateRoutes ~> check {
        status must ===(StatusCodes.BadRequest)
      }
    }
    "reject an empty correlationId when proposing" in {
      val propose =
        s"""
           |{
           |  "correlationId": "",
           |  "data": "HelloWorld",
           |  "to": ["1111", "2222"]
           |}
        """.stripMargin
      val requestPropose =
        Post("/agreements/whatever/propose", HttpEntity(ContentTypes.`application/json`, propose))
      requestPropose ~> certificateRoutes ~> check {
        status must ===(StatusCodes.BadRequest)
      }

    }
    "reject an empty correlationId when agreeing" in {
      val agree =
        s"""
           |{
           |  "correlationId": "",
           |  "data": "HelloWorld"
           |}
        """.stripMargin

      val requestAgree = Post("/agreements/whatever/agree", HttpEntity(ContentTypes.`application/json`, agree))
      requestAgree ~> certificateRoutes ~> check {
        status must ===(StatusCodes.BadRequest)
      }
    }
  }

  "POST /agreements/certificates/decline" should {
    "decline to an item" in {
      val certificate = Certificate("certificateId", "2019/Jan/01")
      val signature = sign(certificate, keys.`private`)
      val body =
        s"""
           |{
           |  "correlationId": "${UUID.randomUUID()}"
           |}
        """.stripMargin

      val request = Post("/agreements/certificates/decline", HttpEntity(ContentTypes.`application/json`, body))

      request ~> certificateRoutes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }

  "POST /agreements/chimeric/agree" should {
    "accept a chimeric transaction" in {
      val tx = ChimericTx(
        List(
          CreateCurrency("ADA")
        )
      )

      val body =
        s"""
           |{
           |  "correlationId": "${UUID.randomUUID()}",
           |  "data": ${Json.toJson(tx).toString()}
           |}
        """.stripMargin

      val request = Post("/agreements/chimeric/agree", HttpEntity(ContentTypes.`application/json`, body))

      request ~> chimericRoutes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }
}

object AgreementsGenericControllerSpec {

  case class Certificate(id: String, date: String)

  implicit val format: Format[Certificate] = Json.format[Certificate]
}
