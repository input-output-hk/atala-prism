package io.iohk.atala.prism.node.cardano.models

import com.google.protobuf.ByteString
import io.circe.{Json, parser}
import io.iohk.atala.prism.identity.PrismDid.{getDEFAULT_MASTER_KEY_ID => masterKeyId}
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata.METADATA_PRISM_INDEX
import io.iohk.atala.prism.protos.{node_internal, node_models}
import org.scalatest.OptionValues._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import scalapb.UnknownFieldSet
import scalapb.UnknownFieldSet.Field

class AtalaObjectMetadataSpec extends AnyWordSpec {
  private val atalaOperations = List(
    node_models.SignedAtalaOperation(
      signedWith = masterKeyId,
      signature = ByteString.copyFrom("Fake signature bytes".getBytes),
      operation = Some(
        node_models.AtalaOperation(operation =
          node_models.AtalaOperation.Operation
            .CreateDid(
              node_models.CreateDIDOperation(didData =
                Some(
                  node_models.CreateDIDOperation.DIDCreationData(
                    publicKeys = List(
                      node_models.PublicKey(
                        id = masterKeyId,
                        usage = node_models.KeyUsage.MASTER_KEY
                      ),
                      node_models.PublicKey(
                        id = "issuing0",
                        usage = node_models.KeyUsage.ISSUING_KEY
                      )
                    )
                  )
                )
              )
            )
        )
      )
    )
  )

  private val atalaObjectWithVersion = node_internal
    .AtalaObject()
    .withBlockContent(
      node_internal.AtalaBlock(
        operations = atalaOperations,
        // Unknown field corresponding to a deprecated field "version", equal to "1"
        unknownFields = UnknownFieldSet.empty.withField(
          1,
          Field(
            Vector(),
            Vector(),
            Vector(),
            Vector(ByteString.copyFromUtf8("1"))
          )
        )
      )
    )
  private val atalaObjectWithoutVersion = node_internal
    .AtalaObject()
    .withBlockContent(
      node_internal.AtalaBlock(
        operations = atalaOperations
      )
    )

  require(
    atalaObjectWithVersion.toByteArray.length > 64,
    "A big object (> 64 bytes) is needed to test proper wrapping of byte strings"
  )

  private val atalaObjectByteStringsWithBlockVersion = List(
    "22450a013112400a076d617374657230121446616b65207369676e61747572652062797465731a1f0a1d0a1b120b0a076d6173746572301001120c0a08697373",
    "75696e67301002"
  )

  private val atalaObjectByteStringsWithoutBlockVersion = List(
    "224212400a076d617374657230121446616b65207369676e61747572652062797465731a1f0a1d0a1b120b0a076d6173746572301001120c0a0869737375696e",
    "67301002"
  )

  "fromTransactionMetadata" should {
    // Test for backward compatibility when we parse deprecated AtalaBlock serialisation from the Cardano blockchain
    "succeed when a valid JSON with the block version" in {
      val result = AtalaObjectMetadata.fromTransactionMetadata(
        TransactionMetadata(
          parseJson(
            s"""{
             |  "21325" : {
             |    "v" : 1,
             |    "c" : [
             |      "${atalaObjectByteStringsWithBlockVersion(0)}",
             |      "${atalaObjectByteStringsWithBlockVersion(1)}"
             |    ]
             |  }
             |}""".stripMargin
          )
        )
      )

      result.value must be(atalaObjectWithVersion)
    }

    "succeed when a valid JSON without block version" in {
      val result = AtalaObjectMetadata.fromTransactionMetadata(
        TransactionMetadata(
          parseJson(
            s"""{
               |  "21325" : {
               |    "v" : 1,
               |    "c" : [
               |      "${atalaObjectByteStringsWithoutBlockVersion(0)}",
               |      "${atalaObjectByteStringsWithoutBlockVersion(1)}"
               |    ]
               |  }
               |}""".stripMargin
          )
        )
      )

      result.value must be(atalaObjectWithoutVersion)
    }

    "fail when index is invalid" in {
      val metadata =
        TransactionMetadata(parseJson("""{ "1": 2 }""".stripMargin))

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when metadata version is invalid" in {
      val metadata = TransactionMetadata(
        parseJson(s"""{
            |"$METADATA_PRISM_INDEX": {
            |  "v": 1337
            |}
            |}""".stripMargin)
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content does not exist" in {
      val metadata = TransactionMetadata(
        parseJson(s"""{
                     |"$METADATA_PRISM_INDEX": {
                     |  "v": 1
                     |}
                     |}""".stripMargin)
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content is of wrong type" in {
      val metadata = TransactionMetadata(
        parseJson(s"""{
                     |"$METADATA_PRISM_INDEX": {
                     |  "v": 1,
                     |  "c": "Other data"
                     |}
                     |}""".stripMargin)
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }

    "fail when content is not a valid AtalaObject" in {
      val metadata = TransactionMetadata(
        parseJson(s"""{
                     |"$METADATA_PRISM_INDEX": {
                     |  "v": 1,
                     |  "c": [
                     |    "0x${atalaObjectByteStringsWithBlockVersion.head}"
                     |  ]
                     |}
                     |}""".stripMargin)
      )

      val result = AtalaObjectMetadata.fromTransactionMetadata(metadata)

      result must be(None)
    }
  }

  "toTransactionMetadata" should {
    "generate correct metadata" in {
      val metadata =
        AtalaObjectMetadata.toTransactionMetadata(atalaObjectWithoutVersion)

      metadata.json.spaces2 must be(
        s"""{
          |  "21325" : {
          |    "map" : [
          |      {
          |        "k" : {
          |          "string" : "v"
          |        },
          |        "v" : {
          |          "int" : 1
          |        }
          |      },
          |      {
          |        "k" : {
          |          "string" : "c"
          |        },
          |        "v" : {
          |          "list" : [
          |            {
          |              "bytes" : "${atalaObjectByteStringsWithoutBlockVersion(
            0
          )}"
          |            },
          |            {
          |              "bytes" : "${atalaObjectByteStringsWithoutBlockVersion(
            1
          )}"
          |            }
          |          ]
          |        }
          |      }
          |    ]
          |  }
          |}""".stripMargin
      )
    }
  }

  private def parseJson(json: String): Json = {
    parser.parse(json).toOption.value
  }
}
