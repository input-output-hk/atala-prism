package io.iohk.atala.prism.node.operations

import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.DataPreparation.dummyLedgerData
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_NO
import io.iohk.atala.prism.node.models.ProtocolVersion.InitialProtocolVersion
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.StateError._
import io.iohk.atala.prism.node.repositories.KeyValuesRepository
import io.iohk.atala.prism.node.repositories.daos.{ProtocolVersionsDAO, PublicKeysDAO}
import io.iohk.atala.prism.node.services.KeyValueService
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.Inside.inside
import org.scalatest.OptionValues._
import tofu.logging.Logging.Make
import tofu.logging.Logging

class ProtocolVersionUpdateOperationSpec extends AtalaWithPostgresSpec {

  val masterKeys = CreateDIDOperationSpec.masterKeys
  val logs: Make[IOWithTraceIdContext] = Logging.Make.plain[IOWithTraceIdContext]

  lazy val proposerDidKeys = List(
    DIDPublicKey(proposerDIDSuffix, "master", KeyUsage.MasterKey, masterKeys.getPublicKey)
  )

  lazy val proposerCreateDIDOperation =
    CreateDIDOperation.parse(CreateDIDOperationSpec.exampleOperation, dummyLedgerData).toOption.value
  lazy val proposerDIDSuffix = proposerCreateDIDOperation.id

  val parsedOperation1: ProtocolVersionUpdateOperation =
    ProtocolVersionUpdateOperation
      .parse(protocolUpdateOperation(ProtocolVersion(2, 0), Some("Second version"), 10), dummyLedgerData)
      .toOption
      .value

  val parsedOperation2: ProtocolVersionUpdateOperation =
    ProtocolVersionUpdateOperation
      .parse(protocolUpdateOperation(ProtocolVersion(2, 1), Some("Second point one version"), 20), dummyLedgerData)
      .toOption
      .value

  def protocolUpdateOperation(
      pv: ProtocolVersion,
      versionName: Option[String],
      effective: Int
  ): node_models.AtalaOperation =
    node_models.AtalaOperation(
      operation = node_models.AtalaOperation.Operation.ProtocolVersionUpdate(
        value = node_models.ProtocolVersionUpdateOperation(
          proposerDid = proposerDIDSuffix.getValue,
          version = Some(
            node_models.ProtocolVersionInfo(
              versionName = versionName.getOrElse(""),
              majorVersion = pv.major,
              minorVersion = pv.minor,
              effectiveSince = effective
            )
          )
        )
      )
    )

  "ProtocolVersionUpdateOperation.parse" should {
    "parse valid ProtocolVersionUpdateOperation AtalaOperation" in {
      ProtocolVersionUpdateOperation.parse(
        protocolUpdateOperation(ProtocolVersion(4, 0), Some("version name"), 20),
        dummyLedgerData
      ) mustBe a[Right[_, _]]
    }

    "return error when proposerDID doesn't have valid form" in {
      val invalidOperation = protocolUpdateOperation(ProtocolVersion(3, 0), None, 13)
        .update(_.protocolVersionUpdate.proposerDid := "invalid DID")

      inside(ProtocolVersionUpdateOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, value, _)) =>
          path.path mustBe Vector("protocolVersionUpdate", "proposerDid")
          value mustBe "invalid DID"
      }
    }

    "return error when major version is negative" in {
      val invalidOperation = protocolUpdateOperation(ProtocolVersion(-3, 0), None, 13)
      inside(ProtocolVersionUpdateOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, message)) =>
          path.path mustBe Vector("protocolVersionUpdate", "version", "majorVersion")
          message mustBe "Negative major version"
      }
    }

    "return error when minor version is negative" in {
      val invalidOperation = protocolUpdateOperation(ProtocolVersion(3, -3), None, 13)
      inside(ProtocolVersionUpdateOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, message)) =>
          path.path mustBe Vector("protocolVersionUpdate", "version", "minorVersion")
          message mustBe "Negative minor version"
      }
    }

    "return error when effectiveSince is negative" in {
      val invalidOperation = protocolUpdateOperation(ProtocolVersion(3, 3), None, -13)
      inside(ProtocolVersionUpdateOperation.parse(invalidOperation, dummyLedgerData)) {
        case Left(ValidationError.InvalidValue(path, _, message)) =>
          path.path mustBe Vector("protocolVersionUpdate", "version", "effectiveSince")
          message mustBe "Negative effectiveSince"
      }
    }
  }

  "ProtocolVersionUpdateOperation.getCorrectnessData" should {
    "provide the key reference be used for signing" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      val CorrectnessData(key, previousOperation) = parsedOperation1
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()
        .toOption
        .value

      key mustBe masterKeys.getPublicKey
      previousOperation mustBe None
    }

    "return state error when an unknown key is used" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      val result = parsedOperation1
        .getCorrectnessData("issuing")
        .transact(database)
        .value
        .unsafeRunSync()

      result mustBe Left(StateError.UnknownKey(proposerDIDSuffix, "issuing"))
    }

    "return state error when a revoked key is used" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      PublicKeysDAO
        .revoke(proposerDIDSuffix, "master", dummyLedgerData)
        .transact(database)
        .unsafeRunSync()

      val result = parsedOperation1
        .getCorrectnessData("master")
        .transact(database)
        .value
        .unsafeRunSync()

      result mustBe Left(StateError.KeyAlreadyRevoked())
    }
  }

  "ProtocolVersionUpdateOperation.applyState" should {
    "create two new protocol updates on the database" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      insertProtocolVersions()

      ProtocolVersionsDAO.getCurrentProtocolVersion
        .transact(database)
        .unsafeRunSync() mustBe InitialProtocolVersion

      ProtocolVersionsDAO.getLastKnownProtocolUpdate
        .transact(database)
        .unsafeRunSync() mustBe parsedOperation2.toProtocolVersionInfo
    }

    "create two new protocol updates on the database. Then make the first of them effective" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      insertProtocolVersions()

      ProtocolVersionsDAO
        .markEffective(10)
        .transact(database)
        .unsafeRunSync() mustBe Some(
        ProtocolVersionInfo(
          parsedOperation1.protocolVersion,
          parsedOperation1.versionName,
          parsedOperation1.effectiveSinceBlockIndex
        )
      )

      ProtocolVersionsDAO.getCurrentProtocolVersion
        .transact(database)
        .unsafeRunSync() mustBe ProtocolVersion(2, 0)

      ProtocolVersionsDAO.getLastKnownProtocolUpdate
        .transact(database)
        .unsafeRunSync() mustBe parsedOperation2.toProtocolVersionInfo
    }

    "return error when a protocol version is not sequential" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      val result2 = parsedOperation2
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value
      result2 mustBe a[NonSequentialProtocolVersion]
    }

    "return error when an effectiveSince is descending" in {
      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      val result = parsedOperation1
        .copy(effectiveSinceBlockIndex = 20)
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val result2 = parsedOperation2
        .copy(effectiveSinceBlockIndex = 10)
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value
      result2 mustBe a[NonAscendingEffectiveSince]
    }

    "return error when an effectiveSince is less than last Cardano block level" in {
      val keyValueService = KeyValueService.unsafe(KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, logs), logs)
      keyValueService.set(LAST_SYNCED_BLOCK_NO, Some(11)).run(TraceId.generateYOLO).unsafeRunSync()

      DataPreparation
        .createDID(DIDData(proposerDIDSuffix, proposerDidKeys, proposerCreateDIDOperation.digest), dummyLedgerData)

      val result1 = parsedOperation1
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
        .left
        .value

      result1 mustBe a[EffectiveSinceNotGreaterThanCurrentCardanoBlockNo]
    }
  }

  def insertProtocolVersions() = {
    val result = parsedOperation1
      .applyState()
      .transact(database)
      .value
      .unsafeToFuture()
      .futureValue
    result mustBe a[Right[_, _]]

    val result2 = parsedOperation2
      .applyState()
      .transact(database)
      .value
      .unsafeToFuture()
      .futureValue
    result2 mustBe a[Right[_, _]]
  }
}
