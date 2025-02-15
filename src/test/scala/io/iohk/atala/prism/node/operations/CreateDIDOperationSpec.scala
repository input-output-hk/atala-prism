package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData, dummyTimestampInfo}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, ProtocolConstants}
import io.iohk.atala.prism.node.operations.StateError.UnsupportedOperation
import io.iohk.atala.prism.node.operations.protocolVersion.SupportedOperations
import io.iohk.atala.prism.node.repositories.daos.ServicesDAO
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation, models}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, CompressedECKeyData}
import org.scalatest.EitherValues._
import org.scalatest.Inside._
import org.scalatest.OptionValues._

object CreateDIDOperationSpec {
  def protoECKeyDataFromPublicKey(
      key: SecpPublicKey
  ): node_models.ECKeyData =
    node_models.ECKeyData(
      curve = key.curveName,
      x = ByteString.copyFrom(key.x),
      y = ByteString.copyFrom(key.y)
    )

  def protoCompressedECKeyDataFromPublicKey(
      key: SecpPublicKey
  ): CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = key.curveName,
      data = ByteString.copyFrom(key.compressed)
    )

  def randomECKeyData: node_models.ECKeyData = {
    val keyPair = CryptoTestUtils.generateKeyPair()
    protoECKeyDataFromPublicKey(keyPair.publicKey)
  }

  def randomCompressedECKeyData: CompressedECKeyData = {
    val keyPair = CryptoTestUtils.generateKeyPair()
    protoCompressedECKeyDataFromPublicKey(keyPair.publicKey)
  }

  val masterKeys: SecpPair = CryptoTestUtils.generateKeyPair()
  val masterEcKeyData: node_models.ECKeyData = protoECKeyDataFromPublicKey(
    masterKeys.publicKey
  )
  val masterCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(masterKeys.publicKey)

  val issuingKeys: SecpPair = CryptoTestUtils.generateKeyPair()
  val issuingEcKeyData: CompressedECKeyData = protoCompressedECKeyDataFromPublicKey(
    issuingKeys.publicKey
  )
  val issuingCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(issuingKeys.publicKey)

  val revokingKeys: SecpPair = CryptoTestUtils.generateKeyPair()
  val revokingEcKeyData: CompressedECKeyData = protoCompressedECKeyDataFromPublicKey(
    revokingKeys.publicKey
  )
  val revokingCompressedEcKeyData: CompressedECKeyData =
    protoCompressedECKeyDataFromPublicKey(
      revokingKeys.publicKey
    )

  private val serviceId1 = "linked-domain1"
  private val serviceId2 = "linked-domain2"

  val exampleOperation: node_models.AtalaOperation = node_models.AtalaOperation(
    node_models.AtalaOperation.Operation.CreateDid(
      value = node_models.CreateDIDOperation(
        didData = Some(
          node_models.CreateDIDOperation.DIDCreationData(
            publicKeys = List(
              node_models.PublicKey(
                "master",
                node_models.KeyUsage.MASTER_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
              ),
              node_models
                .PublicKey(
                  "issuing",
                  node_models.KeyUsage.ISSUING_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.CompressedEcKeyData(issuingEcKeyData)
                ),
              node_models
                .PublicKey(
                  "revoking",
                  node_models.KeyUsage.REVOCATION_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.CompressedEcKeyData(revokingEcKeyData)
                ),
              node_models.PublicKey(
                "authentication",
                node_models.KeyUsage.AUTHENTICATION_KEY,
                Some(
                  node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                ),
                None,
                node_models.PublicKey.KeyData.EcKeyData(randomECKeyData)
              )
            ),
            services = List(
              node_models.Service(
                id = serviceId1,
                `type` = "didCom-credential-exchange",
                serviceEndpoint = "https://baz.example.com/",
                addedOn = None,
                deletedOn = None
              ),
              node_models.Service(
                id = serviceId2,
                `type` = "didCom-chat-message-exchange",
                serviceEndpoint = """{"uri":"https://baz.example.com/"}""",
                addedOn = None,
                deletedOn = None
              )
            ),
            context = List(
              "https://www.w3.org/ns/did/v1",
              "https://w3id.org/security/suites/jws-2020/v1",
              "https://didcomm.org/messaging/contexts/v2",
              "https://identity.foundation/.well-known/did-configuration/v1"
            )
          )
        )
      )
    )
  )

  val exampleOperationWithCompressedKeys: node_models.AtalaOperation =
    node_models.AtalaOperation(
      node_models.AtalaOperation.Operation.CreateDid(
        value = node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = List(
                node_models.PublicKey(
                  "master",
                  node_models.KeyUsage.MASTER_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
                ),
                node_models
                  .PublicKey(
                    "issuing",
                    node_models.KeyUsage.ISSUING_KEY,
                    Some(
                      node_models.LedgerData(timestampInfo =
                        Some(
                          ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)
                        )
                      )
                    ),
                    None,
                    node_models.PublicKey.KeyData
                      .CompressedEcKeyData(issuingCompressedEcKeyData)
                  ),
                node_models
                  .PublicKey(
                    "revoking",
                    node_models.KeyUsage.REVOCATION_KEY,
                    Some(
                      node_models.LedgerData(timestampInfo =
                        Some(
                          ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)
                        )
                      )
                    ),
                    None,
                    node_models.PublicKey.KeyData
                      .CompressedEcKeyData(revokingCompressedEcKeyData)
                  ),
                node_models.PublicKey(
                  "authentication",
                  node_models.KeyUsage.AUTHENTICATION_KEY,
                  Some(
                    node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
                  ),
                  None,
                  node_models.PublicKey.KeyData.CompressedEcKeyData(
                    randomCompressedECKeyData
                  )
                )
              )
            )
          )
        )
      )
    )

}

class CreateDIDOperationSpec extends AtalaWithPostgresSpec {

  import CreateDIDOperationSpec._

  "CreateDIDOperation.parse" should {
    "parse valid CreateDid AtalaOperation" in {
      CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData) mustBe a[Right[_, _]]
    }

    "parse services correctly from valid createDid AtalaOperation" in {
      val parsed = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .value

      val services = parsed.services
      services.size mustBe 2

      services.head.id mustBe serviceId1
      services.head.`type` mustBe "didCom-credential-exchange"
      services.head.serviceEndpoints mustBe exampleOperation.operation.createDid.value.didData.value.services.head.serviceEndpoint

      services.last.id mustBe serviceId2
      services.last.`type` mustBe "didCom-chat-message-exchange"
      services.last.serviceEndpoints mustBe exampleOperation.operation.createDid.value.didData.value.services.last.serviceEndpoint
    }

    "should parse normalized and non normalize URIs as long as they are valid" in {

      val nonNormalized = Vector(
        "https://example.com/home///about",
        "HTTP://EXAMPLE.CoM/home/about",
        "https://example.com/home/about/../services",
        "https://example.com/home/about/../../about",
        "https://example.com/home/././about",
        "telnet://example.com",
        "data:text/plain,Hello%20world!",
        "https://example.com/home/about?a=a1&b=b1",
        "https://example.com/home/about?b=b1&a=a1",
        "https://example.com/home/about?cartoon=tom%26jerry",
        "https://example.com/home/about?cartoon=tom jerry",
        "https://example.com/ho me"
      )

      val normalized = Vector(
        "https://example.com/home/about",
        "http://example.com/home/about",
        "https://example.com/home/services",
        "https://example.com/about",
        "https://example.com/home/about",
        "telnet://example.com/",
        "data:text%2Fplain,Hello%20world!",
        "https://example.com/home/about?a=a1&b=b1",
        "https://example.com/home/about?a=a1&b=b1",
        "https://example.com/home/about?cartoon=tom%26jerry",
        "https://example.com/home/about?cartoon=tom%20jerry",
        "https://example.com/ho%20me"
      )

      val allUris = nonNormalized ++ normalized

      def createUpdatedOperation(uri: String): AtalaOperation = {
        exampleOperation.update(
          _.createDid.didData.services := List(
            node_models.Service(
              id = serviceId1,
              `type` = "didCom-credential-exchange",
              serviceEndpoint = uri,
              addedOn = None,
              deletedOn = None
            )
          )
        )
      }

      for { testCase <- allUris } {
        val op = createUpdatedOperation(testCase)

        val parsed = CreateDIDOperation
          .parse(op, dummyLedgerData)

        parsed mustBe a[Right[_, _]]
      }

    }

    "fail to parse services if one of the services has invalid id" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = "not a valid URI fragment",
            `type` = "didCom-credential-exchange",
            serviceEndpoint = "https://baz.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "id")
        case Right(_) => fail("Failed to validate invalid service id")
      }

    }

    "fail to parse services if one of the services id char length is over the limit" in {
      val idCharLenLimit = ProtocolConstants.idCharLenLimit
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1 + ("a" * idCharLenLimit),
            `type` = "didCom-credential-exchange",
            serviceEndpoint = "https://baz.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "id")
        case Right(_) => fail("Failed to validate invalid service id")
      }
    }

    "fail to parse services if the amount of services is over the limit" in {
      val servicesLimit = ProtocolConstants.servicesLimit
      val services = Array
        .tabulate(servicesLimit + 1) { index =>
          node_models.Service(
            id = serviceId1 + index.toString,
            `type` = "didCom-credential-exchange",
            serviceEndpoint = "https://baz.example.com",
            addedOn = None,
            deletedOn = None
          )
        }
        .toList

      val updated = exampleOperation.update(
        _.createDid.didData.services := services
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services")
        case Right(_) => fail("Failed to validate invalid service id")
      }

    }

    "fail to parse services if one of the services has valid id but with whitespace" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = s" $serviceId1",
            `type` = "didCom-credential-exchange",
            serviceEndpoint = "https://baz.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "id")
        case Right(_) => fail("Failed to validate invalid service id")
      }

    }

    "fail to parse type if it is valid JSON but one of the types is empty" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """["valid type", ""]""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "type", "1")
        case Right(_) => fail("Failed to validate invalid service type")
      }
    }

    "fail to parse type if it is valid JSON array but empty" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """[]""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "type")
        case Right(_) => fail("Failed to validate invalid service type")
      }
    }

    "fail to parse type if it is valid JSON but one of the types contains chars that are not allowed" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """["valid type", "hello world!"]""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "type", "1")
        case Right(_) => fail("Failed to validate invalid service type")
      }
    }

    "parse type string correctly when it is null, true or false" in {
      val updated1 = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """null""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val updated2 = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """false""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val updated3 = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """true""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed1 = CreateDIDOperation
        .parse(updated1, dummyLedgerData)
        .value
      val parsed2 = CreateDIDOperation
        .parse(updated2, dummyLedgerData)
        .value
      val parsed3 = CreateDIDOperation
        .parse(updated3, dummyLedgerData)
        .value

      val services1 = parsed1.services
      services1.length mustBe 1
      services1.head.`type` mustBe """null"""

      val services2 = parsed2.services
      services2.length mustBe 1
      services2.head.`type` mustBe """false"""

      val services3 = parsed3.services
      services3.length mustBe 1
      services3.head.`type` mustBe """true"""

    }

    "parse the type correctly when it is a one character numeric string" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = "3",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)
        .value

      val services = parsed.services
      services.length mustBe 1
      services.head.`type` mustBe "3"

    }

    "parse the type correctly when it is a JSON array and one of the elements is one character numeric string" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """["valid type", 3]""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)
        .value

      val services = parsed.services
      services.length mustBe 1
      services.head.`type` mustBe """["valid type", 3]"""
    }

    "parse the type correctly when string is several space separated words with multiply spaces" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = """abc   abc abc abc""",
            serviceEndpoint = "https://foo.example.com",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)
        .value

      val services = parsed.services
      services.length mustBe 1
      services.head.`type` mustBe """abc   abc abc abc"""
    }

    "fail to parse services if one of the service endpoints of any service is not a valid URI" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = "didCom-credential-exchange",
            serviceEndpoint = """["https://foo.example.com","not a valid URI"]""",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "serviceEndpoint", "1")
        case Right(_) => fail("Failed to validate invalid service endpoint")
      }
    }

    "fail to parse services if one of the service endpoints of any service is valid but has whitespace" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = "didCom-credential-exchange",
            serviceEndpoint = """
              ["https://foo.example.com",
              " https://bar.example.com"]
            """,
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "serviceEndpoint", "1")
        case Right(_) => fail("Failed to validate invalid service endpoint")
      }
    }

    "fail to parse services when JSON is valid but is an empty array" in {
      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = "didCom-credential-exchange",
            serviceEndpoint = "[]",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "serviceEndpoint")
        case Right(_) => fail("Failed to validate invalid service endpoint")
      }
    }

    "fail to parse services if type of one of the services is empty" in {
      val typesToCheck = List("  ", "", "\n", "\t")

      val parsedServices = typesToCheck.map { tp =>
        val updated = exampleOperation.update(
          _.createDid.didData.services := List(
            node_models.Service(
              id = serviceId1,
              `type` = tp,
              serviceEndpoint = "https://baz.example.com",
              addedOn = None,
              deletedOn = None
            )
          )
        )

        CreateDIDOperation
          .parse(updated, dummyLedgerData)
      }

      parsedServices.foreach { parsedService =>
        inside(parsedService) {
          case Left(ValidationError.MissingValue(path)) =>
            path.path mustBe Vector("createDid", "didData", "services", "0", "type")
          case Right(_) => fail("Failed to validate invalid service type")
        }
      }
    }

    "fail to parse services if service endpoints are empty" in {

      val updated = exampleOperation.update(
        _.createDid.didData.services := List(
          node_models.Service(
            id = serviceId1,
            `type` = "some type",
            serviceEndpoint = "",
            addedOn = None,
            deletedOn = None
          )
        )
      )

      val parsedService = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsedService) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "services", "0", "serviceEndpoint")
        case Right(_) => fail("Failed to validate invalid service type")
      }
    }

    "return error when a key has missing curve name" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).ecKeyData.curve := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "ecKeyData",
            "curve"
          )
      }
    }

    "return error when a key has missing x" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.publicKeys(0).ecKeyData.x := ByteString.EMPTY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "ecKeyData",
            "x"
          )
      }
    }

    "return error when a key has missing data" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .keyData := node_models.PublicKey.KeyData.Empty
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.MissingValue(path)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "keyData"
          )
      }
    }

    "return error when a key has missing id" in {
      val invalidOperation = exampleOperation
        .update(_.createDid.didData.publicKeys(0).id := "")

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "id"
          )
      }
    }

    "return error when a key has invalid id" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.publicKeys(0).id := "it isn't proper key id"
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "id"
          )
      }
    }

    "return error when a key has invalid usage" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .usage := node_models.KeyUsage.UNKNOWN_KEY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys",
            "0",
            "usage"
          )
      }
    }

    "return error when master key doesn't exist" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .usage := node_models.KeyUsage.ISSUING_KEY
        )

      inside(CreateDIDOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector(
            "createDid",
            "didData",
            "publicKeys"
          )
      }
    }

    "return error when some of the context strings are not valid URIs" in {
      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.context.modify(current => "invalid URI" +: current)
        )

      val parsedOperation = CreateDIDOperation.parse(invalidOperation, dummyLedgerData)

      inside(parsedOperation) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "context", "0")
        case Right(_) => fail("Failed to validate invalid context strings")
      }
    }

    "return error when some of the context strings length is over allowed limit" in {

      val charLimit = ProtocolConstants.contextStringCharLimit
      // valid URI that is longer them max char limit
      val invalidLengthUri = "https://www.example.com/" + "a" * (charLimit - 23)

      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.context.modify(current => invalidLengthUri +: current)
        )

      val parsedOperation = CreateDIDOperation.parse(invalidOperation, dummyLedgerData)

      inside(parsedOperation) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "context", "0")
        case Right(_) => fail("Failed to validate invalid context strings")
      }
    }

    "return error when context contains duplicate strings" in {

      val invalidOperation = exampleOperation
        .update(
          _.createDid.didData.context.modify(current => current.head +: current)
        )

      val parsedOperation = CreateDIDOperation.parse(invalidOperation, dummyLedgerData)

      inside(parsedOperation) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "context")
        case Right(_) => fail("Failed to validate invalid context strings")
      }
    }

    "return error when one of the keys id char length is over the limit" in {
      val idCharLenLimit = ProtocolConstants.idCharLenLimit

      val updated = exampleOperation
        .update(
          _.createDid.didData
            .publicKeys(0)
            .id := "issuing" + ("a" * idCharLenLimit)
        )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys", "0", "id")
        case Right(_) => fail("Failed to validate invalid service id")
      }
    }

    "return error when the amount of public keys is over the limit" in {
      val publicKeysLimit = ProtocolConstants.publicKeysLimit
      val pks = Array
        .tabulate(publicKeysLimit + 1) { index =>
          node_models.PublicKey(
            "issuing" + index.toString,
            node_models.KeyUsage.ISSUING_KEY,
            Some(
              node_models.LedgerData(timestampInfo = Some(ProtoCodecs.toTimeStampInfoProto(dummyTimestampInfo)))
            ),
            None,
            node_models.PublicKey.KeyData.EcKeyData(masterEcKeyData)
          )
        }
        .toList

      val updated = exampleOperation.update(
        _.createDid.didData.publicKeys := pks
      )

      val parsed = CreateDIDOperation
        .parse(updated, dummyLedgerData)

      inside(parsed) {
        case Left(ValidationError.InvalidValue(path, _, _)) =>
          path.path mustBe Vector("createDid", "didData", "publicKeys")
        case Right(_) => fail("Failed to validate invalid service id")
      }

    }

  }

  "CreateDIDOperation.getCorrectnessData" should {
    "provide the key to be used for signing" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value
      val CorrectnessData(key, previousOperation) = parsedOperation
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key.compressed.toVector mustBe masterKeys.publicKey.compressed.toVector
      previousOperation mustBe None
    }
  }

  private def toDIDPublicKey(keyState: DIDPublicKeyState): DIDPublicKey = {
    DIDPublicKey(
      didSuffix = keyState.didSuffix,
      keyId = keyState.keyId,
      keyUsage = keyState.keyUsage,
      key = keyState.key
    )
  }

  "CreateDIDOperation.applyState" should {

    "create the DID information in the database" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val didState = DataPreparation.findByDidSuffix(parsedOperation.id)

      didState.didSuffix mustBe parsedOperation.id
      didState.lastOperation mustBe parsedOperation.digest
      didState.keys.map(
        toDIDPublicKey
      ) must contain theSameElementsAs parsedOperation.keys

      didState.context.sorted mustBe parsedOperation.context.sorted

      for (key <- parsedOperation.keys) {
        val keyState =
          DataPreparation.findKey(parsedOperation.id, key.keyId).value
        DIDPublicKey(
          keyState.didSuffix,
          keyState.keyId,
          keyState.keyUsage,
          keyState.key
        ) mustBe key
        keyState.addedOn.timestampInfo mustBe dummyLedgerData.timestampInfo
        keyState.revokedOn mustBe None
      }
    }

    "create service in database" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      val foundService = ServicesDAO.get(parsedOperation.id, serviceId1).transact(database).unsafeRunSync()

      foundService.nonEmpty mustBe true

      val expectedServiceEndpoints = "https://baz.example.com/"

      foundService.nonEmpty mustBe true
      foundService.get.id mustBe serviceId1
      foundService.get.`type` mustBe "didCom-credential-exchange"
      foundService.get.serviceEndpoints mustBe expectedServiceEndpoints
    }

    "return error when given DID already exists" in {
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      DataPreparation
        .createDID(
          DIDData(parsedOperation.id, Nil, Nil, Nil, parsedOperation.digest),
          dummyLedgerData
        )

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue

      inside(result.left.value) { case StateError.EntityExists(tpe, _) =>
        tpe mustBe "DID"
      }
    }

    "return error when CreateDID operation is not supported by the current protocol" in {
      // Let's pretend that none of operations are supported
      val fakeSupportedOperations: SupportedOperations =
        (_: Operation, _: models.ProtocolVersion) => false
      val parsedOperation = CreateDIDOperation
        .parse(exampleOperation, dummyLedgerData)
        .toOption
        .value

      val result = parsedOperation
        .applyState(dummyApplyOperationConfig)(fakeSupportedOperations)
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value
      result mustBe a[UnsupportedOperation]
    }
  }
}
