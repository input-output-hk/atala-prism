package io.iohk.atala.prism.node.repositories.daos

import doobie.util.transactor.Transactor
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.nodeState.{DIDServiceEndpointState, DIDServiceState, LedgerData}
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

  private val serviceId1 = "did:prism:123#linked-domain1"
  private val serviceId2 = "did:prism:123#linked-domain2"
  private val services = List(
    DIDService(
      id = serviceId1,
      didSuffix = didSuffix,
      `type` = "didCom-credential-exchange",
      serviceEndpoints = List(
        DIDServiceEndpoint(
          url = "https://foo.example.com",
          urlIndex = 0
        ),
        DIDServiceEndpoint(
          url = "https://bar.example.com",
          urlIndex = 1
        )
      )
    ),
    DIDService(
      id = serviceId2,
      didSuffix = didSuffix,
      `type` = "didCom-chat-message-exchange",
      serviceEndpoints = List(
        DIDServiceEndpoint(
          url = "https://baz.example.com",
          urlIndex = 0
        ),
        DIDServiceEndpoint(
          url = "https://qux.example.com",
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

  private def validateStandardSelection(expected: List[DIDService], got: List[DIDServiceState]): Unit = {
    expected.size mustBe 2

    val first = got.find(_.id == serviceId1).value
    val second = got.find(_.id == serviceId2).value

    first.id mustBe expected.head.id
    first.didSuffix mustBe expected.head.didSuffix
    first.`type` mustBe expected.head.`type`
    first.serviceEndpoints.size mustBe expected.head.serviceEndpoints.size
    first.serviceEndpoints.head.serviceId mustBe first.serviceId
    first.serviceEndpoints.zipWithIndex.foreach { seAndIndex: (DIDServiceEndpointState, Int) =>
      val (se, index) = seAndIndex
      se.urlIndex mustBe expected.head.serviceEndpoints(index).urlIndex
      se.url mustBe expected.head.serviceEndpoints(index).url
    }

    second.id mustBe expected.last.id
    second.didSuffix mustBe expected.last.didSuffix
    second.`type` mustBe expected.last.`type`
    second.serviceEndpoints.size mustBe expected.last.serviceEndpoints.size
    second.serviceEndpoints.last.serviceId mustBe second.serviceId
    second.serviceEndpoints.zipWithIndex.foreach { seAndIndex: (DIDServiceEndpointState, Int) =>
      val (se, index) = seAndIndex
      se.urlIndex mustBe expected.last.serviceEndpoints(index).urlIndex
      se.url mustBe expected.last.serviceEndpoints(index).url
    }

  }

  "ServicesDAO" should {
    "ServicesDAO.getAllActiveByDidSuffix should retrieve all inserted services" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      validateStandardSelection(services, receivedServices)

    }

    "ServicesDAO.getAllActiveByDidSuffix should retrieve services without services endpoints correctly" in {
      // Note: this scenario should not be possible, other tests verify that no service can be created without service endpoints
      // however this test tests that selection and grouping of service endpoints by service_id works correctly in this scenario as well.
      val services1 = DIDService(
        id = "did:prism:123#linked-domain0",
        didSuffix = didSuffix,
        `type` = "no-service-endpoints",
        serviceEndpoints = Nil
      ) :: services

      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services1, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      validateStandardSelection(services, receivedServices)

      val serviceWithoutServiceEndpoints = receivedServices.find(_.id == "did:prism:123#linked-domain0").value

      serviceWithoutServiceEndpoints.serviceEndpoints.isEmpty mustBe true
      serviceWithoutServiceEndpoints.`type` mustBe "no-service-endpoints"

    }

    "ServicesDAO.getAllActiveByDidSuffix should not retrieve revoked services" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      ServicesDAO.revokeService(didSuffix, serviceId1, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedServices.size mustBe 1
      val expectedService = services.find(_.id == serviceId2).value
      val receivedService = receivedServices.head

      receivedService.revokedOn.isEmpty mustBe true
      receivedService.`type` mustBe expectedService.`type`

    }

    "ServicesDAO.getAllActiveByDidSuffix should return an empty list if there are not services for a did" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, Nil, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedServices.size mustBe 0
    }

    "ServicesDAO.get should return a single service by didSuffix and id" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedService = ServicesDAO.get(didSuffix, serviceId1).transact(xa).unsafeRunSync()
      val expectedService = services.find(_.id == serviceId1).value
      receivedService.value.id mustBe expectedService.id
      receivedService.value.`type` mustBe expectedService.`type`
    }

    "ServicesDAO.get should return None if service with specified id and didSuffix does not exist" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedService = ServicesDAO.get(didSuffix, "whatever").transact(xa).unsafeRunSync()
      receivedService.isEmpty mustBe true
    }

    "ServicesDAO.get should not retrieve revoked service" in {
      val xa = implicitly[Transactor[IO]]
      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      ServicesDAO.revokeService(didSuffix, serviceId1, dummyLedgerData).transact(xa).unsafeRunSync()
      val receivedService = ServicesDAO.get(didSuffix, serviceId1).transact(xa).unsafeRunSync()
      receivedService.isEmpty mustBe true

    }

    "ServicesDAO.insert should insert a record in services table and records in service endpoints table" in {
      val xa = implicitly[Transactor[IO]]

      val didData = DIDData(didSuffix, keys, Nil, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      receivedServices.isEmpty mustBe true

      ServicesDAO.insert(services.head, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedServicesAfterInsertion = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()

      receivedServicesAfterInsertion.size mustBe 1
      val receivedServiceAfterInsertion = receivedServicesAfterInsertion.head

      receivedServiceAfterInsertion.id mustBe services.head.id
      receivedServiceAfterInsertion.`type` mustBe services.head.`type`

    }

    "ServicesDAO.insert should not insert a record if associated did does not exist in db beforehand" in {
      val xa = implicitly[Transactor[IO]]

      val error = intercept[org.postgresql.util.PSQLException] {
        ServicesDAO.insert(services.head, dummyLedgerData).transact(xa).unsafeRunSync()
      }

      error.getMessage.contains("services_did_suffix_fk") mustBe true
    }

    "ServicesDAO.revokeAllServices should revoke all services of the did" in {
      val xa = implicitly[Transactor[IO]]

      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      receivedServices.size mustBe 2

      ServicesDAO.revokeAllServices(didSuffix, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedServicesAfterRevokaction = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      receivedServicesAfterRevokaction.size mustBe 0

    }

    "ServicesDAO.revokeService should revoke a specific service of a DID by id" in {
      val xa = implicitly[Transactor[IO]]

      val didData = DIDData(didSuffix, keys, services, operationDigest)
      DataPreparation.createDID(didData, dummyLedgerData)(xa)

      ServicesDAO.revokeService(didSuffix, serviceId1, dummyLedgerData).transact(xa).unsafeRunSync()

      val receivedServices = ServicesDAO.getAllActiveByDidSuffix(didSuffix).transact(xa).unsafeRunSync()
      receivedServices.size mustBe 1

      receivedServices.head.id mustBe serviceId2
    }

  }
}
