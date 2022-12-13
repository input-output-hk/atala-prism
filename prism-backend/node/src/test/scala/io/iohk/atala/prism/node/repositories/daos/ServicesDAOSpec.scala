package io.iohk.atala.prism.node.repositories.daos

import doobie.util.transactor.Transactor
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.nodeState.{DIDServiceEndpointState, LedgerData}
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, DIDService, DIDServiceEndpoint, KeyUsage}
import io.iohk.atala.prism.node.repositories.{didSuffixFromDigest, digestGen}
import io.iohk.atala.prism.protos.models.TimestampInfo
import org.scalatest.OptionValues._

import java.time.Instant

class ServicesDAOSpec extends AtalaWithPostgresSpec {

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "communication",
      keyUsage = KeyUsage.CommunicationKey,
      key = EC.generateKeyPair().getPublicKey
    )
  )

  val services = List(
    DIDService(
      id = "did:prism:123#linked-domain1",
      didSuffix = didSuffix,
      `type` = "didCom-credential-exchange",
      serviceEndpoints = List(
        DIDServiceEndpoint(
          url = "https://bar.example.com",
          urlIndex = 0
        ),
        DIDServiceEndpoint(
          url = "https://baz.example.com",
          urlIndex = 1
        )
      )
    ),
    DIDService(
      id = "did:prism:123#linked-domain2",
      didSuffix = didSuffix,
      `type` = "didCom-chat-message-exchange",
      serviceEndpoints = List(
        DIDServiceEndpoint(
          url = "https://qux.example.com",
          urlIndex = 0
        ),
        DIDServiceEndpoint(
          url = "https://baz.example.com",
          urlIndex = 1
        )
      )
    )
  )

  val dummyTimestamp =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "ServicesDAO" should {
    "findByDidSuffix should retrieve all inserted services" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)
      println("inserted")
      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedServices.size mustBe 2

      println(receivedServices)

      val first = receivedServices.find(_.id == services.head.id).value
      val second = receivedServices.find(_.id == services.last.id).value

      first.id mustBe services.head.id
      first.didSuffix mustBe services.head.didSuffix
      first.`type` mustBe services.head.`type`
      first.serviceEndpoints.size mustBe services.head.serviceEndpoints.size
      first.serviceEndpoints.head.serviceId mustBe first.serviceId
      first.serviceEndpoints.zipWithIndex.foreach { seAndIndex: (DIDServiceEndpointState, Int) =>
        val (se, index) = seAndIndex
        se.urlIndex mustBe services.head.serviceEndpoints(index).urlIndex
        se.url mustBe services.head.serviceEndpoints(index).url
      }

      second.id mustBe services.last.id
      second.didSuffix mustBe services.last.didSuffix
      second.`type` mustBe services.last.`type`
      second.serviceEndpoints.size mustBe services.last.serviceEndpoints.size
      second.serviceEndpoints.last.serviceId mustBe second.serviceId
      second.serviceEndpoints.zipWithIndex.foreach { seAndIndex: (DIDServiceEndpointState, Int) =>
        val (se, index) = seAndIndex
        se.urlIndex mustBe services.last.serviceEndpoints(index).urlIndex
        se.url mustBe services.last.serviceEndpoints(index).url
      }

    }

  }
}
