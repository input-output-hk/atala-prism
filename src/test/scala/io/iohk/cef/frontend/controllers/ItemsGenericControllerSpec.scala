package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}
import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.transactionservice.Envelope
import scala.concurrent.duration._

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with SigningKeyPairs {

  import Codecs._
  import ItemsGenericControllerSpec._

  implicit val executionContext = system.dispatcher
  val service = mock[DataItemService[BirthCertificate]]

  when(service.processAction(any()))
    .thenAnswer(new Answer[Either[ApplicationError, DataItemServiceResponse]] {
      override def answer(i: InvocationOnMock): Either[ApplicationError, DataItemServiceResponse] =
        i.getArguments()(0) match {
          case Envelope(_: DataItemAction.InsertAction[_], _, _) =>
            Right(DataItemServiceResponse.DIUnit)
          case Envelope(_: DataItemAction.ValidateAction[_], _, _) =>
            Right(DataItemServiceResponse.Validation(true))
          case Envelope(_: DataItemAction.DeleteAction[_], _, _) =>
            Right(DataItemServiceResponse.DIUnit)
        }
    })

  val controller = new ItemsGenericController

  "POST /certificates" should {

    implicit val canValidate = new CanValidate[DataItem[BirthCertificate]] {
      override def validate(t: DataItem[BirthCertificate]): Either[ApplicationError, Unit] = Right(Unit)
    }
    lazy val routes =
      controller.routes[BirthCertificate]("birth-certificates", service, 30 seconds)

    "create an item" in {
      val body =
        """
          |{
          | "content": {
          |   "id":"birth-cert",
          |   "data":
          |     {
          |       "date": "01/01/2015",
          |       "name": "Input Output HK"
          |     },
          |   "witnesses":[],
          |   "owners":[]
          |  },
          |  "containerId": "nothing",
          |  "destinationDescriptor": {
          |    "type": "everyone",
          |    "obj": {}
          |  }
          |}
        """.stripMargin
      val request = Post("/birth-certificates", HttpEntity(ContentTypes.`application/json`, body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)

        val json = responseAs[JsValue]
        println(json)
      }
    }
  }

  "DELETE /certificates" should {

    implicit val canValidate: CanValidate[DataItem[BirthCertificate]] =
      (t: DataItem[BirthCertificate]) => Right(())

    lazy val routes =
      controller.routes[BirthCertificate]("birth-certificates", service)

    "delete an item" in {
      val ownerKeyPair = bob
      val signature = {
        import io.iohk.cef.crypto._
        val unsigned: BirthCertificate =
          BirthCertificate("01/01/2015", "Input Output HK")
        sign(unsigned, ownerKeyPair.`private`).toCompactString
      }

      val deleteBody =
        s"""
          |{
          | "content": {
          |   "id":"birth-cert",
          |   "signature":"$signature"
          |  },
          |  "containerId": "nothing",
          |  "destinationDescriptor": {
          |    "type": "everyone",
          |    "obj": {}
          |  }
          |}
        """.stripMargin
      val deleteRequest = Post("/birth-certificates/delete", HttpEntity(ContentTypes.`application/json`, deleteBody))

      deleteRequest ~> routes ~> check {

        val json = responseAs[JsValue]
        status must ===(StatusCodes.OK)
        json.toString must ===("{}")
      }

    }

    "validate an item" in {
      val ownerKeyPair = bob
      val body =
        s"""
          |{
          | "content": {
          |   "id":"birth-cert",
          |   "data":
          |     {
          |       "date": "01/01/2015",
          |       "name": "Input Output HK"
          |     },
          |   "witnesses":[],
          |   "owners":[{"key": "${ownerKeyPair.public.toCompactString}"}]
          |  },
          |  "containerId": "nothing",
          |  "destinationDescriptor": {
          |    "type": "everyone",
          |    "obj": {}
          |  }
          |}
        """.stripMargin
      val validateRequest = Post("/birth-certificates/validation", HttpEntity(ContentTypes.`application/json`, body))

      validateRequest ~> routes ~> check {

        val json = responseAs[JsValue]
        status must ===(StatusCodes.OK)
        json.toString must ===("""{"isValid":true}""")
      }
    }

  }

}

object ItemsGenericControllerSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
