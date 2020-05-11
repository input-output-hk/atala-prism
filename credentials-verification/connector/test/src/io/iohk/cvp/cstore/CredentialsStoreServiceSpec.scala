package io.iohk.cvp.cstore

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.connector.repositories.{ConnectionsRepository, RequestNoncesRepository}
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, StoreUser}
import io.iohk.cvp.cstore.repositories.daos.{IndividualsDAO, StoreUsersDAO, StoredCredentialsDAO}
import io.iohk.cvp.cstore.services.{StoreIndividualsService, StoredCredentialsService}
import io.iohk.cvp.grpc.GrpcAuthenticationHeaderParser
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.{cstore_api, cstore_models}
import org.mockito.MockitoSugar._

import scala.concurrent.duration._

class CredentialsStoreServiceSpec extends RpcSpecBase {

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

  val usingApiAs = usingApiAsConstructor(
    new cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _)
  )

  lazy val individuals = new StoreIndividualsService(database)
  lazy val storedCredentials = new StoredCredentialsService(database)
  private lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = new SignedRequestsAuthenticator(
    connectionsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )
  lazy val verifierId = ParticipantId("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services =
    Seq(
      cstore_api.CredentialsStoreServiceGrpc
        .bindService(
          new CredentialsStoreService(individuals, storedCredentials, authenticator),
          executionContext
        )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    StoreUsersDAO
      .insert(StoreUser(verifierId))
      .transact(database)
      .unsafeToFuture()
      .futureValue

    ParticipantsDAO
      .insert(ParticipantInfo(verifierId, ParticipantType.Verifier, None, "Verifier", None, None))
      .transact(database)
      .unsafeToFuture()
      .futureValue
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
          .list(verifierId, None, 1)
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
          .list(verifierId, None, 1)
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
        val individualId =
          serviceStub.createIndividual(cstore_api.CreateIndividualRequest("Individual")).individual.get.individualId

        val issuerDid = "did:atala:7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39"
        val proofId = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
        val request =
          cstore_api.StoreCredentialRequest(individualId, issuerDid, proofId, ByteString.EMPTY, ByteString.EMPTY)
        val response = serviceStub.storeCredential(request)

        val credential = StoredCredentialsDAO
          .getFor(verifierId, ParticipantId(individualId))
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.proofId mustBe proofId
        credential.issuerDid mustBe issuerDid
        credential.content must be(empty)
        credential.signature must be(empty)
      }
    }
  }

  "getCredentialsFor" should {
    "get credentials for individual" in {
      usingApiAs(verifierId) { serviceStub =>
        val individualId =
          serviceStub.createIndividual(cstore_api.CreateIndividualRequest("Individual")).individual.get.individualId

        val issuerDid = "did:atala:7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39"
        val proofId = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
        serviceStub.storeCredential(
          cstore_api.StoreCredentialRequest(individualId, issuerDid, proofId, ByteString.EMPTY, ByteString.EMPTY)
        )

        val request = cstore_api.GetStoredCredentialsForRequest(individualId)
        val response = serviceStub.getStoredCredentialsFor(request)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.issuerDid mustBe issuerDid
        credential.proofId mustBe proofId
        credential.content.toByteArray must be(empty)
        credential.signature.toByteArray must be(empty)
      }
    }
  }
}
