package io.iohk.cvp.cstore

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.connector.model.{ParticipantInfo, ParticipantType}
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, StoreUser}
import io.iohk.cvp.cstore.protos.CredentialsStoreServiceGrpc
import io.iohk.cvp.cstore.repositories.daos.{IndividualsDAO, StoreUsersDAO, StoredCredentialsDAO}
import io.iohk.cvp.cstore.services.{StoreIndividualsService, StoreUsersService, StoredCredentialsService}
import io.iohk.cvp.models.ParticipantId
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class CredentialsStoreServiceSpec extends RpcSpecBase {

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

  override val tables =
    List("stored_credentials", "store_individuals", "store_users", "connection_tokens", "participants", "connections")

  val usingApiAs = usingApiAsConstructor(new CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _))

  lazy val storeUsers = new StoreUsersService(database)
  lazy val individuals = new StoreIndividualsService(database)
  lazy val storedCredentials = new StoredCredentialsService(database)
  private lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.nodenew.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = new SignedRequestsAuthenticator(connectionsRepository, nodeMock)
  lazy val verifierId = ParticipantId("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services = Seq(
    CredentialsStoreServiceGrpc
      .bindService(
        new CredentialsStoreService(storeUsers, individuals, storedCredentials, authenticator),
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

  "register" should {
    "create relevant records in the database" in {
      usingApiAs.unlogged { serviceStub =>
        val request = protos.RegisterRequest("Verifier", ByteString.EMPTY)
        val result = serviceStub.register(request)

        val user = StoreUsersDAO.get(ParticipantId(result.userId)).transact(database).unsafeRunSync().value
        // user needs just to exist

        val participant =
          ParticipantsDAO.findBy(ParticipantId(result.userId)).transact(database).value.unsafeRunSync().value
        participant.tpe mustBe ParticipantType.Verifier
        participant.name mustBe "Verifier"
        participant.logo mustBe None
      }
    }
  }

  "createIndividual" should {
    "create individual in the database" in {
      usingApiAs(verifierId) { serviceStub =>
        val request = protos.CreateIndividualRequest("Individual Individual", "individual@email.org")
        val result = serviceStub.createIndividual(request)

        result.individual.get.fullName mustBe "Individual Individual"
        result.individual.get.email mustBe "individual@email.org"
        result.individual.get.status mustBe protos.IndividualConnectionStatus.CREATED

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
          val request = protos.CreateIndividualRequest(individual)
          serviceStub.createIndividual(request)
        }

        val obtainedIndividualsStream = Stream.iterate((Seq.empty[protos.Individual], true)) {
          case (individuals, _) =>
            val request = protos.GetIndividualsRequest(
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
          val request = protos.CreateIndividualRequest(individual)
          serviceStub.createIndividual(request)
        }
        val request = protos.GetIndividualsRequest(
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
          serviceStub.createIndividual(protos.CreateIndividualRequest("Individual")).individual.get.individualId

        val request = protos.GenerateConnectionTokenForRequest(individualId = individualId)
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
          serviceStub.createIndividual(protos.CreateIndividualRequest("Individual")).individual.get.individualId

        val issuerDid = "did:atala:7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39"
        val proofId = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
        val request =
          protos.StoreCredentialRequest(individualId, issuerDid, proofId, ByteString.EMPTY, ByteString.EMPTY)
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
          serviceStub.createIndividual(protos.CreateIndividualRequest("Individual")).individual.get.individualId

        val issuerDid = "did:atala:7cd7b833ba072944ab6579da20706301ec6ab863992a41ae9d80d56d14559b39"
        val proofId = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
        serviceStub.storeCredential(
          protos.StoreCredentialRequest(individualId, issuerDid, proofId, ByteString.EMPTY, ByteString.EMPTY)
        )

        val request = protos.GetStoredCredentialsForRequest(individualId)
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
