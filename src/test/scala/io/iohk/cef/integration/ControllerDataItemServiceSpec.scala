package io.iohk.cef.integration

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.Envelope
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.controllers.ItemsGenericController
import io.iohk.cef.frontend.controllers.common.Codecs
import io.iohk.cef.network.{MessageStream, Network}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, Json}

class ControllerDataItemServiceSpec
    extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

  implicit val executionContext = system.dispatcher
  import Codecs._
  import ControllerDataItemServiceSpec._

  val table: Table = mock[Table]
  val network: Network[Envelope[DataItemAction[BirthCertificate]]] =
    mock[Network[Envelope[DataItemAction[BirthCertificate]]]]
  implicit val canValidate = new CanValidate[DataItem[BirthCertificate]] {
    override def validate(t: DataItem[BirthCertificate]): Either[ApplicationError, Unit] = Right(Unit)
  }
  when(table.insert(any(), any())(any[NioEncDec[BirthCertificate]], any[CanValidate[DataItem[BirthCertificate]]]()))
    .thenReturn(Right(()))
  val messageStream = mock[MessageStream[Envelope[DataItemAction[BirthCertificate]]]]
  when(network.messageStream).thenReturn(messageStream)
  val service = new DataItemService[BirthCertificate](table, network)

  val controller = new ItemsGenericController

  "POST /certificate" should {

    lazy val routes =
      controller.routes[BirthCertificate]("certificate", service)

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

      val request = Post("/certificate", HttpEntity(ContentTypes.`application/json`, body))

      request ~> routes ~> check {
        status must ===(StatusCodes.Created)
      }
      verify(table, times(1)).insert(any(), any())(any(), any())
      verify(network, times(1)).disseminateMessage(any())

    }
  }
}

object ControllerDataItemServiceSpec {

  case class BirthCertificate(date: String, name: String)

  implicit val birthCertificateFormat: Format[BirthCertificate] = Json.format[BirthCertificate]

}
