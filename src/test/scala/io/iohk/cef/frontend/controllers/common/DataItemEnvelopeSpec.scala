package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.transactionservice._
import io.iohk.cef.data.{DataItem, Owner, Witness}
import io.iohk.cef.network.NodeId
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, Json}

class DataItemEnvelopeSpec extends WordSpec with MustMatchers {

  import DataItemEnvelopeSpec._
  import Codecs._

  "DataItemEnvelopeFormat" should {
    "serialize and deserialize" in {

      val destinationDescriptor: DestinationDescriptor = Or(
        a = Not(Everyone),
        b = And(
          a = SingleNode(NodeId("AB".getBytes)),
          b = SetOfNodes(Set(NodeId("IO".getBytes())))
        )
      )
      val dataItem = DataItem("custom", Data("IOHK"), Seq.empty[Witness], Seq.empty[Owner])
      val input = Envelope(
        content = dataItem,
        containerId = "nothing",
        destinationDescriptor = destinationDescriptor
      )

      val serialized = Json.toJson(input)
      val deserialized = serialized.as[Envelope[DataItem[Data]]]
      deserialized mustEqual input
    }
  }
}

object DataItemEnvelopeSpec {

  case class Data(name: String)

  implicit val dataFormat: Format[Data] = Json.format[Data]
}
