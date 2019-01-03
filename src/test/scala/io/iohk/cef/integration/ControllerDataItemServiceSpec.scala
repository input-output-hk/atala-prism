package io.iohk.cef.integration

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.QueryEngine
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.ItemsGenericController
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.transactionservice.Envelope
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration._

class ControllerDataItemServiceSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  implicit val executionContext = system.dispatcher
  import Codecs._
  import ControllerDataItemServiceSpec._

  val table: Table[BirthCertificate] = mock[Table[BirthCertificate]]
  val network: Network[Envelope[DataItemAction[BirthCertificate]]] =
    mock[Network[Envelope[DataItemAction[BirthCertificate]]]]
  implicit val canValidate = new CanValidate[DataItem[BirthCertificate]] {
    override def validate(t: DataItem[BirthCertificate]): Either[ApplicationError, Unit] = Right(Unit)
  }
  when(table.insert(any[DataItem[BirthCertificate]])(any())).thenReturn(Right(()))
  val messageStream = mock[MessageStream[Envelope[DataItemAction[BirthCertificate]]]]
  when(network.messageStream).thenReturn(messageStream)
  val service = new DataItemService[BirthCertificate](table, network, mock[QueryEngine[BirthCertificate]])

  val controller = new ItemsGenericController

  "POST /certificate" should {

    lazy val routes =
      controller.routes[BirthCertificate]("certificate", service, 30 seconds)

    "create an item" in {
      val keys = generateSigningKeyPair()
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
          |   "witnesses":[],
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

      val request = Post("/certificate", HttpEntity(ContentTypes.`application/json`, body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
      verify(table, times(1)).insert(any())(any())
      verify(network, times(1)).disseminateMessage(any())

    }
  }
}

object ControllerDataItemServiceSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
