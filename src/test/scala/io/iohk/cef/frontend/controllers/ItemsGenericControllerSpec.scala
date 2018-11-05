package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.frontend.models.DataItemEnvelope
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._

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
    override def insert[I](tableId: TableId, dataItem: DataItem[I])(
        implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Unit] = {
      Right(())
    }
  }

  val controller = new ItemsGenericController(service)

  "POST /certificates" should {
    type Envelope = DataItemEnvelope[BirthCertificate, BirthCertificateItem]
    lazy val routes = controller.routes[BirthCertificate, BirthCertificateItem, Envelope]("birth-certificates")

    "create an item" in {
      val body =
        """
          |{
          |  "content": {
          |    "data": {
          |      "date": "01/01/2015",
          |      "name": "Input Output HK"
          |    }
          |  },
          |  "tableId": "nothing",
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

  case class BirthCertificateItem(override val data: BirthCertificate) extends DataItem[BirthCertificate] {
    override def id: String = "birth-certificate"

    override def witnesses: Seq[Witness] = Seq.empty

    override def owners: Seq[Owner] = Seq.empty

    override def apply(): Either[DataItemError, Unit] = Right(())
  }

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

  implicit val birthCertificateItemFormat: Format[BirthCertificateItem] = Json.format[BirthCertificateItem]

}
