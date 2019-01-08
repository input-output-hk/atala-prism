package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.test.DummyMessageStream
import io.iohk.cef.transactionservice.Envelope
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}

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
      val keys = alice
      val data = BirthCertificate("01/01/2015", "Input Output HK")
      val signature = sign(LabeledItem.Create(data), keys.`private`)
      val body =
        s"""
          |{
          | "content": {
          |   "id":"birth-cert",
          |   "data": {
          |       "date": "${data.date}",
          |       "name": "${data.name}"
          |   },
          |   "witnesses": [],
          |   "owners": [
          |     {
          |       "key": "${keys.public.toCompactString()}",
          |       "signature": "${signature.toCompactString()}"
          |     }
          |   ]
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
      _ => Right(())

    lazy val routes =
      controller.routes[BirthCertificate]("birth-certificates", service, 30 seconds)

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
      val keys = bob
      val data = BirthCertificate("01/01/2015", "Input Output HK")
      val signature = sign(LabeledItem.Create(data), keys.`private`)
      val body =
        s"""
           |{
           | "content": {
           |   "id":"birth-cert",
           |   "data": {
           |       "date": "${data.date}",
           |       "name": "${data.name}"
           |   },
           |   "witnesses": [],
           |   "owners": [
           |     {
           |       "key": "${keys.public.toCompactString()}",
           |       "signature": "${signature.toCompactString()}"
           |     }
           |   ]
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

  "GET /certificates" should {
    implicit val canValidate: CanValidate[DataItem[BirthCertificate]] = _ => Right(Unit)
    lazy val routes =
      controller.routes[BirthCertificate]("birth-certificates", service, 30 seconds)

    val emptyObservable = Observable.empty[Either[ApplicationError, Seq[DataItem[BirthCertificate]]]]
    val monixScheduler: TestScheduler = TestScheduler.apply()
    val queryResult = new DummyMessageStream(emptyObservable)(monixScheduler)
    when(service.processQuery(any())).thenReturn(queryResult)

    "succeed" in {
      val queryBody =
        """
          |{
          |  "type": "predicateQuery",
          |  "value": {
          |        "type" : "eqPredicate",
          |        "value" : {
          |          "field" : {
          |            "index" : 2
          |          },
          |          "value" : {
          |            "type" : "stringRef",
          |            "value" : {
          |              "value" : "string"
          |            }
          |          }
          |        }
          |      }
          |}
          """.stripMargin

      val body =
        s"""
          |{
          |  "containerId": "1",
          |  "destinationDescriptor": {
          |    "type": "everyone",
          |    "obj": {}
          |  },
          |  "content": $queryBody
          |}
        """.stripMargin
      val request = Get("/birth-certificates", HttpEntity(ContentTypes.`application/json`, body))
      request ~> routes ~> check {
        status must ===(StatusCodes.OK)
      }
    }
  }
}

object ItemsGenericControllerSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
