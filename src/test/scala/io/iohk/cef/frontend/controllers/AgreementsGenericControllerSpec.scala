package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.agreements.AgreementsService
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.DataItem
import io.iohk.cef.ledger.chimeric.{ChimericTx, CreateCurrency}
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar._
import play.api.libs.json.{Format, Json}

class AgreementsGenericControllerSpec
    extends WordSpec
    with ScalatestRouteTest
    with PlayJsonSupport {

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
          |  "correlationId": "agreementId",
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
          |  "recipients": ["1111", "2222"]
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
           |  "correlationId": "agreementId",
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
           |  "correlationId": "agreementId",
           |  "data": ${Json.toJson(tx).toString()},
           |  "recipients": ["1111", "2222"]
           |}
        """.stripMargin

      val request = Post("/agreements/chimeric/propose", HttpEntity(ContentTypes.`application/json`, body))

      request ~> chimericRoutes ~> check {
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
           |  "correlationId": "agreementId",
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
