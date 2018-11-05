package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.core._
import io.iohk.cef.data.{DataItem, DataItemError, Owner, Witness}
import io.iohk.cef.frontend.models.DataItemEnvelope
import io.iohk.cef.network.NodeId
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, Json}

class DataItemEnvelopeSpec extends WordSpec with MustMatchers {

  import Codecs._
  import DataItemEnvelopeSpec._

  "DataItemEnvelopeFormat" should {
    "serialize and deserialize" in {
      type Envelope = DataItemEnvelope[Data, CustomItem]

      val destinationDescriptor: DestinationDescriptor = Or(
        a = Not(Everyone),
        b = And(
          a = SingleNode(NodeId("AB".getBytes)),
          b = SetOfNodes(Set(NodeId("IO".getBytes())))
        )
      )
      val input = new Envelope(
        content = CustomItem("custom", Data("IOHK")),
        tableId = "nothing",
        destinationDescriptor = destinationDescriptor
      )

      val serialized = Json.toJson(input)
      val deserialized = serialized.as[Envelope]
      deserialized mustEqual input
    }
  }
}

object DataItemEnvelopeSpec {

  case class Data(name: String)
  case class CustomItem(override val id: String, override val data: Data) extends DataItem[Data] {

    override def witnesses: Seq[Witness] = Seq.empty

    override def owners: Seq[Owner] = Seq.empty

    override def apply(): Either[DataItemError, Unit] = Right(())
  }

  implicit val dataFormat: Format[Data] = Json.format[Data]
  implicit val customItemFormat: Format[CustomItem] = Json.format[CustomItem]
}
