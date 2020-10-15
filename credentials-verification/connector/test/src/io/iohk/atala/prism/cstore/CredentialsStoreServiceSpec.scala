package io.iohk.atala.prism.cstore

import java.util.UUID

import doobie.implicits._
import io.circe.Json
import io.iohk.atala.prism.connector.model.{ConnectionId, ParticipantLogo, ParticipantType, TokenString}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.console.models.{Contact, Institution}
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, StoredCredentialsDAO}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, StoredCredentialsRepository}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.cstore.models.IndividualConnectionStatus
import io.iohk.atala.prism.cstore.repositories.IndividualsRepository
import io.iohk.atala.prism.cstore.repositories.daos.IndividualsDAO
import io.iohk.atala.prism.cstore.services.CredentialsStoreService
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.{Ledger, ParticipantId, TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.{cstore_api, cstore_models}
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class CredentialsStoreServiceSpec extends RpcSpecBase {

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

  val usingApiAs = usingApiAsConstructor(
    new cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _)
  )

  lazy val individuals = new IndividualsRepository(database)
  lazy val storedCredentials = new StoredCredentialsRepository(database)
  private lazy val holdersRepository = new ContactsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )
  lazy val verifierId = ParticipantId("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services =
    Seq(
      cstore_api.CredentialsStoreServiceGrpc
        .bindService(
          new CredentialsStoreService(individuals, storedCredentials, holdersRepository, authenticator),
          executionContext
        )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    new ParticipantsRepository(database)
      .create(
        CreateParticipantRequest(
          verifierId,
          ParticipantType.Verifier,
          "Verifier",
          "did:prism:test",
          ParticipantLogo(Vector()),
          TransactionInfo(TransactionId.from(SHA256Digest.compute("id".getBytes).value).value, Ledger.InMemory)
        )
      )
      .value
      .futureValue
    ()
  }

  "createIndividual" should {
    "create individual in the database" in {
      usingApiAs(verifierId) { serviceStub =>
        val request = cstore_api.CreateIndividualRequest("Individual Individual", "individual@email.org")
        val result = serviceStub.createIndividual(request)

        result.individual.get.fullName mustBe "Individual Individual"
        result.individual.get.email mustBe "individual@email.org"
        result.individual.get.status mustBe cstore_models.IndividualConnectionStatus.CREATED

        val individual = IndividualsDAO
          .listIndividuals(verifierId, None, 1)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        individual.id.uuid.toString mustBe result.individual.get.individualId
        individual.status mustBe IndividualConnectionStatus.Created
        individual.fullName mustBe "Individual Individual"
        individual.email mustBe Some("individual@email.org")
      }
    }
  }

  "createHolder" should {
    "create a holder in the database" in {
      usingApiAs(verifierId) { serviceStub =>
        val json = Json.obj(
          "name" -> Json.fromString("Individual Individual"),
          "email" -> Json.fromString("individual@email.org")
        )
        val request = cstore_api.CreateHolderRequest(json.noSpaces)

        val result = serviceStub.createHolder(request).holder.value
        result.jsonData must be(json.noSpaces)
        result.status mustBe cstore_models.IndividualConnectionStatus.CREATED
        result.connectionToken must be(empty)
        result.connectionId must be(empty)
        result.holderId mustNot be(empty)
      }
    }
  }

  "getIndividuals" should {
    "return individuals ordered by creation date" in {
      val individualNames = (1 to 9).map(i => i.toString)
      usingApiAs(verifierId) { serviceStub =>
        for (individual <- individualNames) {
          val request = cstore_api.CreateIndividualRequest(individual)
          serviceStub.createIndividual(request)
        }

        val obtainedIndividualsStream = Stream.iterate((Seq.empty[cstore_models.Individual], true)) {
          case (individuals, _) =>
            val request = cstore_api.GetIndividualsRequest(
              lastSeenIndividualId = individuals.lastOption.map(_.individualId).getOrElse(""),
              limit = 2
            )
            val response = serviceStub.getIndividuals(request)
            response.individuals.size must be <= 2
            (individuals ++ response.individuals, response.individuals.nonEmpty)
        }

        val obtainedIndividuals = obtainedIndividualsStream.takeWhile(_._2).last._1

        obtainedIndividuals.map(_.fullName) mustBe individualNames
      }
    }

    "return first individuals when referenced individual does not exist" in {
      val individualNames = (1 to 9).map(i => i.toString)
      usingApiAs(verifierId) { serviceStub =>
        for (individual <- individualNames) {
          val request = cstore_api.CreateIndividualRequest(individual)
          serviceStub.createIndividual(request)
        }
        val request = cstore_api.GetIndividualsRequest(
          lastSeenIndividualId = "85caaf82-9dac-4e80-824c-44284648f696",
          limit = 2
        )
        val response = serviceStub.getIndividuals(request)
        response.individuals.toList.map(_.fullName) mustBe individualNames.take(2)
      }
    }
  }

  "generateConnectionTokenFor" should {
    "generate token for individual" in {
      usingApiAs(verifierId) { serviceStub =>
        val individualId =
          serviceStub.createIndividual(cstore_api.CreateIndividualRequest("Individual")).individual.get.individualId

        val request = cstore_api.GenerateConnectionTokenForRequest(individualId = individualId)
        val response = serviceStub.generateConnectionTokenFor(request)

        val individual = IndividualsDAO
          .listIndividuals(verifierId, None, 1)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head
        individual.connectionToken mustBe Some(response.token)
        individual.status mustBe IndividualConnectionStatus.Invited
      }
    }
  }

  "storeCredential" should {
    "store credential in the database" in {
      usingApiAs(verifierId) { serviceStub =>
        val individualId = Contact.Id(
          UUID.fromString(
            serviceStub
              .createIndividual(cstore_api.CreateIndividualRequest("Individual"))
              .individual
              .get
              .individualId
          )
        )
        val connectionToken =
          serviceStub
            .generateConnectionTokenFor(cstore_api.GenerateConnectionTokenForRequest(individualId.value.toString))
            .token
        val mockConnectionId = ConnectionId(UUID.randomUUID())
        ContactsDAO
          .setConnectionAsAccepted(Institution.Id(verifierId.uuid), TokenString(connectionToken), mockConnectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue

        val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
        val request =
          cstore_api.StoreCredentialRequest(mockConnectionId.id.toString, encodedSignedCredential)
        serviceStub.storeCredential(request)

        val credential = StoredCredentialsDAO
          .getStoredCredentialsFor(Institution.Id(verifierId.uuid), individualId)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe individualId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }
  }

  "getCredentialsFor" should {
    "get credentials for individual" in {
      usingApiAs(verifierId) { serviceStub =>
        val individualId =
          serviceStub.createIndividual(cstore_api.CreateIndividualRequest("Individual")).individual.get.individualId

        val connectionToken =
          serviceStub.generateConnectionTokenFor(cstore_api.GenerateConnectionTokenForRequest(individualId)).token
        val mockConnectionId = ConnectionId(UUID.randomUUID())
        ContactsDAO
          .setConnectionAsAccepted(Institution.Id(verifierId.uuid), TokenString(connectionToken), mockConnectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue

        val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"

        serviceStub.storeCredential(
          cstore_api.StoreCredentialRequest(mockConnectionId.id.toString, encodedSignedCredential)
        )

        val request = cstore_api.GetStoredCredentialsForRequest(individualId)
        val response = serviceStub.getStoredCredentialsFor(request)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.individualId mustBe individualId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }
  }

  "getHolders" should {

    def holder(i: Int): Json =
      Json.obj(
        "name" -> Json.fromString(s"Holder $i"),
        "email" -> Json.fromString(s"holder_$i@email.org")
      )
    val holders = for (i <- 0 to 9) yield holder(i)

    "return holders ordered by creation date" in {
      usingApiAs(verifierId) { serviceStub =>
        for (h <- holders) {
          val request = cstore_api.CreateHolderRequest(jsonData = h.noSpaces)
          serviceStub.createHolder(request)
        }

        val request = cstore_api.GetHoldersRequest(limit = holders.size)
        val response = serviceStub.getHolders(request)
        val obtainedHolders = response.holders
        obtainedHolders.size mustBe holders.size
        obtainedHolders.map(_.jsonData) mustBe holders.map(_.noSpaces)
      }
    }

    "return first holders when referenced holder does not exist" in {
      usingApiAs(verifierId) { serviceStub =>
        for (h <- holders) {
          val request = cstore_api.CreateHolderRequest(jsonData = h.noSpaces)
          serviceStub.createHolder(request)
        }
        val request = cstore_api.GetHoldersRequest(
          lastSeenHolderId = UUID.randomUUID().toString,
          limit = 2
        )
        val response = serviceStub.getHolders(request)
        response.holders.toList.map(_.jsonData) mustBe holders.take(2).map(_.noSpaces)
      }
    }
  }
}
