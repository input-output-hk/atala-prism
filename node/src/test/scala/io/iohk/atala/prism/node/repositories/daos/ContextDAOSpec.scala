package io.iohk.atala.prism.node.repositories.daos

import doobie.util.transactor.Transactor
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.repositories.{didSuffixFromDigest, digestGen}
import io.iohk.atala.prism.protos.models.TimestampInfo
import identus.apollo.MyKeyPair
import org.scalatest.OptionValues._

import java.time.Instant

class ContextDAOSpec extends AtalaWithPostgresSpec {

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  def keys: List[DIDPublicKey] = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "keyAgreement",
      keyUsage = KeyUsage.KeyAgreementKey,
      key = MyKeyPair.generateKeyPair.publicKey
    )
  )

  private val context = List(
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/suites/jws-2020/v1",
    "https://didcomm.org/messaging/contexts/v2",
    "https://identity.foundation/.well-known/did-configuration/v1"
  ).sorted

  private val didData = DIDData(didSuffix, keys, Nil, context, operationDigest)

  val dummyTimestamp =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "ContextDAO" should {
    "ContextDAO.getAllActiveByDidSuffix should retrieve all inserted context strings" in {
      val xa = implicitly[Transactor[IO]]
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedContext = ContextDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedContext.sorted mustBe context

    }

    "ContextDAO.getAllActiveByDidSuffix should not retrieve revoked context strings" in {
      val xa = implicitly[Transactor[IO]]
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      ContextDAO.revokeAllContextStrings(didSuffix, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedContext = ContextDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedContext.isEmpty mustBe true

    }

    "ContextDAO.revokeAllContextStrings should revoke context string of a particular did" in {
      val xa = implicitly[Transactor[IO]]
      val operationDigest2 = digestGen(0, 2)
      val didSuffix2 = didSuffixFromDigest(operationDigest2)

      val didData2 = didData.copy(
        didSuffix = didSuffix2,
        keys = keys.map(_.copy(didSuffix = didSuffix2)),
        lastOperation = operationDigest2
      )

      // create 2 dids, each have context strings
      DataPreparation.createDID(didData, dummyLedgerData)(xa)
      DataPreparation.createDID(didData2, dummyLedgerData)(xa)

      ContextDAO.revokeAllContextStrings(didSuffix, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedContextOfRevoked = ContextDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      val receivedContextOfNonRevoked = ContextDAO.getAllActiveByDidSuffix(didSuffix2).transact(xa).unsafeRunSync()

      receivedContextOfRevoked.isEmpty mustBe true
      receivedContextOfNonRevoked.sorted mustBe context

    }

    "ContextDAO.insert should insert context strings into a contexts table" in {
      val xa = implicitly[Transactor[IO]]
      DataPreparation.createDID(didData.copy(context = Nil), dummyLedgerData)(xa)

      val receivedContext = ContextDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      receivedContext.isEmpty mustBe true

      ContextDAO.insert(context.head, didSuffix, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedContextAfterInsertion = ContextDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedContextAfterInsertion.size mustBe 1
      receivedContextAfterInsertion.head mustBe context.head

    }

  }
}
