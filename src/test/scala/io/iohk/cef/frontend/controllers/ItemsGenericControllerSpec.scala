package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.data.{CanValidate, DataItem, DataItemService, Table}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.DataItemEnvelope
import org.scalatest.mockito.MockitoSugar.mock

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import Codecs._
  import ItemsGenericControllerSpec._

  implicit val executionContext = system.dispatcher
  val service = new DataItemService(mock[Table]) {
    override def insert[I](dataItem: DataItem[I])(
        implicit itemSerializable: NioEncDec[I],
        canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
      Right(())
    }
  }

  val controller = new ItemsGenericController(service)

  "POST /certificates" should {

    implicit val canValidate = new CanValidate[DataItem[BirthCertificate]] {
      override def validate(t: DataItem[BirthCertificate]): Either[ApplicationError, Unit] = Right(Unit)
    }
    lazy val routes =
      controller.routes[BirthCertificate, DataItemEnvelope[DataItem[BirthCertificate]]]("birth-certificates")

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
}

object ItemsGenericControllerSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
