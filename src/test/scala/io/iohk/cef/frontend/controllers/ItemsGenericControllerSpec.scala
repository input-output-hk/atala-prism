package io.iohk.cef.frontend.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsValue, Json}

import scala.util.Try

class ItemsGenericControllerSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  import ItemsGenericControllerSpec._

  implicit val executionContext = system.dispatcher

  val service = new DataItemService(null) {
    override def insert[I](dataItem: DataItem[I])(
        implicit itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Unit] = {
      Right(())
    }
  }

  val controller = new ItemsGenericController(service)

  "POST /certificates" should {

    lazy val routes = controller.routes[BirthCertificate, BirthCertificateItem]("birth-certificates")

    "create an item" in {
      val body =
        """
          |{
          |  "data": {
          |    "date": "01/01/2015",
          |    "name": "Input Output HK"
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

  implicit val serializable: ByteStringSerializable[BirthCertificate] = new ByteStringSerializable[BirthCertificate] {
    override def decode(u: ByteString): Option[BirthCertificate] = {
      def f = Try {
        Json.parse(new String(u.toArray)).asOpt[BirthCertificate]
      }

      f.toOption.flatten
    }

    override def encode(t: BirthCertificate): ByteString = {
      val x = Json.toJson(t).toString().getBytes()
      ByteString(x)
    }
  }

}
