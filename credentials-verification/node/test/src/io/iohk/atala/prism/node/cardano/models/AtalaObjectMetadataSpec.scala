package io.iohk.atala.prism.node.cardano.models

import io.circe.Json
import io.iohk.atala.prism.protos.node_internal
import org.scalatest.matchers.must.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.wordspec.AnyWordSpec

class AtalaObjectMetadataSpec extends AnyWordSpec {
  private val atalaObject = node_internal
    .AtalaObject()
    .withBlock(node_internal.AtalaObject.Block.BlockContent(node_internal.AtalaBlock().withVersion("1")))

  "fromTransactionMetadata" should {

    "succeed when valid" in {
      val metadata = AtalaObjectMetadata.toTransactionMetadata(atalaObject)

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result.value must be(atalaObject)
    }

    "fail when index is invalid" in {
      val metadata = TransactionMetadata(Json.obj("wrong" -> Json.obj()))

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when version is invalid" in {
      val metadata = TransactionMetadata(
        Json.obj(
          AtalaObjectMetadata.METADATA_PRISM_INDEX.toString -> Json
            .obj(AtalaObjectMetadata.VERSION_KEY -> Json.fromInt(1337))
        )
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content does not exist" in {
      val metadata = TransactionMetadata(
        Json.obj(
          AtalaObjectMetadata.METADATA_PRISM_INDEX.toString -> Json
            .obj(AtalaObjectMetadata.VERSION_KEY -> Json.fromInt(AtalaObjectMetadata.METADATA_VERSION))
        )
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content is of wrong type" in {
      val metadata = TransactionMetadata(
        Json.obj(
          AtalaObjectMetadata.METADATA_PRISM_INDEX.toString -> Json
            .obj(
              AtalaObjectMetadata.VERSION_KEY -> Json.fromInt(AtalaObjectMetadata.METADATA_VERSION),
              AtalaObjectMetadata.CONTENT_KEY -> Json.fromString("Other data")
            )
        )
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content is not a valid AtalaObject" in {
      val metadata = TransactionMetadata(
        Json.obj(
          AtalaObjectMetadata.METADATA_PRISM_INDEX.toString -> Json
            .obj(
              AtalaObjectMetadata.VERSION_KEY -> Json.fromInt(AtalaObjectMetadata.METADATA_VERSION),
              AtalaObjectMetadata.CONTENT_KEY -> Json.arr(Json.fromInt(1), Json.fromInt(2), Json.fromInt(3))
            )
        )
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }
  }

  "toTransactionMetadata" should {
    "generate correct metadata" in {
      val metadata = AtalaObjectMetadata.toTransactionMetadata(atalaObject)

      metadata.json.spaces2 must be("""{
          |  "21325" : {
          |    "version" : 1,
          |    "content" : [
          |      34,
          |      3,
          |      10,
          |      1,
          |      49
          |    ]
          |  }
          |}""".stripMargin)
    }
  }
}
