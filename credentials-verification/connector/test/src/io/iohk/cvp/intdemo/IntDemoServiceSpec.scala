package io.iohk.cvp.intdemo

import credential.{Credential, ProofRequest}
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import io.iohk.cvp.intdemo.IntDemoServiceSpec._
import io.iohk.cvp.intdemo.Testing.{eventually, neverEver}
import io.iohk.cvp.intdemo.protos.SubjectStatus.{CONNECTED, CREDENTIAL_SENT, UNCONNECTED}
import io.iohk.cvp.intdemo.protos._
import io.iohk.cvp.models.ParticipantId
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.ArgumentMatcher
import org.mockito.MockitoSugar.{after, mock, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture}
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class IntDemoServiceSpec extends FlatSpec {

  implicit val pc: PatienceConfig = PatienceConfig(1 second, 100 millis)

  "getConnectionToken" should "create a connection token via the connector" in intDemoService {
    (connectorIntegration, repository, intDemoService) =>
      when(connectorIntegration.generateConnectionToken(issuerId)).thenReturn(Future(token))
      when(repository.mergeSubjectStatus(token, UNCONNECTED)).thenReturn(Future(1))

      val tokenResponse = intDemoService.getConnectionToken(GetConnectionTokenRequest()).futureValue

      tokenResponse.connectionToken shouldBe token.token
  }

  val nonEmittingStates = Table(
    ("Current status", "Connection", "User Info", "Expected response"),
    (UNCONNECTED, None, None, UNCONNECTED),
    (UNCONNECTED, Some(connection), None, CONNECTED),
    (UNCONNECTED, None, Some(userInfo), UNCONNECTED),
    (CONNECTED, Some(connection), None, CONNECTED),
    (CREDENTIAL_SENT, Some(connection), Some(userInfo), CREDENTIAL_SENT)
  )

  forAll(nonEmittingStates) { (currentStatus, connection, userInfo, expectedResponse) =>
    "getSubjectStatusStream" should s"return $expectedResponse given " +
      s"current status is $currentStatus, " +
      s"wallet is connected is ${connection.isDefined} and " +
      s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
      currentStatus,
      connection,
      userInfo
    ) { (connectorIntegration, _, intDemoService) =>
      val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]
      intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)
      scheduler.tick(1 second)
      verify(streamObserver, eventually.times(1)).onNext(GetSubjectStatusResponse(expectedResponse))
      verify(connectorIntegration, neverEver).sendCredential(any[ParticipantId], any[ConnectionId], any[Credential])
      verify(streamObserver, neverEver).onError(any)
    }
  }

  val emittingStates = Table(
    ("Current status", "Connection", "User Info", "Expected response"),
    (UNCONNECTED, Some(connection), Some(userInfo), CREDENTIAL_SENT),
    (CONNECTED, Some(connection), Some(userInfo), CREDENTIAL_SENT)
  )

  forAll(emittingStates) { (currentStatus, connection, userInfo, expectedResponse) =>
    "getSubjectStatusStream" should s"emit credential and return $expectedResponse given " +
      s"current status is $currentStatus, " +
      s"wallet is connected is ${connection.isDefined} and " +
      s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
      currentStatus,
      connection,
      userInfo
    ) { (connectorIntegration, _, intDemoService) =>
      val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]
      intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)
      scheduler.tick(1 second)

      verify(connectorIntegration, eventually.times(1))
        .sendCredential(eqTo(issuerId), eqTo(connectionId), credentialMatcher)
      verify(streamObserver, eventually.times(1)).onCompleted()
      verify(streamObserver, neverEver).onError(any)
    }
  }

  val illegalStates = Table(
    ("Current status", "Connection", "User Info"),
    (CONNECTED, None, None),
    (CONNECTED, None, Some(userInfo)),
    (CREDENTIAL_SENT, None, None),
    (CREDENTIAL_SENT, None, Some(userInfo)),
    (CREDENTIAL_SENT, Some(connection), None)
  )

  forAll(illegalStates) { (currentStatus, connection, userInfo) =>
    "getSubjectStatusStream" should s"report an error given " +
      s"current status is $currentStatus, " +
      s"wallet is connected is ${connection.isDefined} and " +
      s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
      currentStatus,
      connection,
      userInfo
    ) { (connectorIntegration, _, intDemoService) =>
      val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]
      intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)
      scheduler.tick(1 second)

      verify(connectorIntegration, neverEver).sendCredential(any[ParticipantId], any[ConnectionId], any[Credential])
      verify(streamObserver, eventually.times(1)).onError(any[IllegalStateException])
    }
  }

  "getSubjectStatusStream" should "handle callback errors by terminating" in intDemoService { (_, _, intDemoService) =>
    val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]

    when(streamObserver.onNext(any[GetSubjectStatusResponse])).thenThrow(new RuntimeException("timeout or something"))

    intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)
    scheduler.tick(1 second)
    scheduler.tick(1 second)

    verify(streamObserver, after(100).atMost(1)).onNext(any[GetSubjectStatusResponse])
  }

  "getSubjectStatusStream" should s"emit proof requests return CONNECTED when a user connects" in intDemoService(
    UNCONNECTED,
    Some(connection),
    None
  ) { (connectorIntegration, _, intDemoService) =>
    val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]
    intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)
    scheduler.tick(1 second)

    verify(connectorIntegration, eventually.times(1))
      .sendProofRequest(eqTo(issuerId), eqTo(connectionId), proofRequestMatcher)
    verify(streamObserver, eventually.times(1)).onNext(GetSubjectStatusResponse(CONNECTED))
    verify(streamObserver, neverEver).onError(any)
  }
}

object IntDemoServiceSpec {

  implicit val ec: ExecutionContext = ExecutionContext.global
  import monix.execution.schedulers.TestScheduler

  private val scheduler = TestScheduler()
  private val connectionId = ConnectionId.random()
  private val messageId = MessageId.random()
  private val issuerId = IdServiceImpl.issuerId
  private val token = new TokenString("a token")
  private val connection = Connection(connectionToken = token, connectionId = connectionId)
  private val userInfo = "X"
  private val credential = Credential("type-id", "credential-document")
  private val proofRequest = ProofRequest(typeId = "type-id", connectionToken = token.token)

  def intDemoService(testCode: (ConnectorIntegration, IntDemoRepository, IntDemoService[String]) => Any): Unit = {
    intDemoService(UNCONNECTED, None, None)(testCode)
  }

  def intDemoService(
      subjectStatus: SubjectStatus,
      connection: Option[Connection],
      requiredData: Option[String]
  )(testCode: (ConnectorIntegration, IntDemoRepository, IntDemoService[String]) => Any): Unit = {
    val connectorIntegration = mock[ConnectorIntegration]
    val repository = mock[IntDemoRepository]

    when(connectorIntegration.sendCredential(eqTo(issuerId), eqTo(connectionId), any)).thenReturn(Future(messageId))
    when(connectorIntegration.sendProofRequest(eqTo(issuerId), eqTo(connectionId), any)).thenReturn(Future(messageId))
    when(connectorIntegration.getConnectionByToken(token)).thenReturn(Future(connection))
    when(repository.mergeSubjectStatus(eqTo(token), any)).thenReturn(Future(1))
    when(repository.findSubjectStatus(token)).thenReturn(Future(Some(subjectStatus)))

    val proofRequestIssuer: Connection => Future[Unit] = { connection =>
      connectorIntegration.sendProofRequest(issuerId, connection.connectionId, proofRequest).map(_ => ())
    }

    val service = new IntDemoService[String](
      issuerId = issuerId,
      connectorIntegration = connectorIntegration,
      intDemoRepository = repository,
      schedulerPeriod = 1 second,
      requiredDataLoader = _ => Future(requiredData),
      proofRequestIssuer = proofRequestIssuer,
      getCredential = _ => credential,
      scheduler = scheduler
    )

    testCode(connectorIntegration, repository, service)
  }

  private def credentialMatcher: Credential = {
    argThat(new ArgumentMatcher[Credential] {
      override def matches(c: Credential): Boolean = {
        c == credential
      }
    })
  }

  private def proofRequestMatcher: ProofRequest = {
    argThat(new ArgumentMatcher[ProofRequest] {
      override def matches(p: ProofRequest): Boolean = {
        println(s"Executing proof request matcher for $p")
        p == proofRequest
      }
    })
  }
}
