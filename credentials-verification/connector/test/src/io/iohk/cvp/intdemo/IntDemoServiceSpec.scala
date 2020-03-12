package io.iohk.cvp.intdemo

import credential.Credential
import io.grpc.stub.StreamObserver
import io.iohk.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import io.iohk.cvp.intdemo.IntDemoServiceSpec._
import io.iohk.cvp.intdemo.Testing.{eventually, neverEver}
import io.iohk.cvp.intdemo.protos.SubjectStatus.{CONNECTED, CREDENTIAL_SENT, UNCONNECTED}
import io.iohk.cvp.intdemo.protos._
import io.iohk.cvp.models.ParticipantId
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.ArgumentMatcher
import org.mockito.MockitoSugar.{mock, verify, when, after}
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

      verify(streamObserver, eventually.atLeastOnce).onNext(GetSubjectStatusResponse(expectedResponse))
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

      verify(connectorIntegration, neverEver).sendCredential(any[ParticipantId], any[ConnectionId], any[Credential])
      verify(streamObserver, eventually.atLeastOnce).onError(any[IllegalStateException])
    }
  }

  "getSubjectStatusStream" should "handle callback errors by terminating" in intDemoService { (_, _, intDemoService) =>
    val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]

    when(streamObserver.onNext(any[GetSubjectStatusResponse])).thenThrow(new RuntimeException("timeout or something"))

    intDemoService.getSubjectStatusStream(GetSubjectStatusRequest(token.token), streamObserver)

    verify(streamObserver, after(100).atMost(1)).onNext(any[GetSubjectStatusResponse])
  }
}

object IntDemoServiceSpec {

  implicit val ec: ExecutionContext = ExecutionContext.global
  import monix.execution.Scheduler.{global => scheduler}
  private val connectionId = ConnectionId.random()
  private val messageId = MessageId.random()
  private val issuerId = IdServiceImpl.issuerId
  private val token = new TokenString("a token")
  private val connection = Connection(connectionToken = token, connectionId = connectionId)
  private val userInfo = "X"
  private val credential = Credential("type-id", "credential-document")

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

    val service = new IntDemoService[String](
      issuerId,
      connectorIntegration,
      repository,
      schedulerPeriod = 1 milli,
      _ => Future(requiredData),
      _ => credential,
      scheduler
    )

    when(connectorIntegration.sendCredential(eqTo(issuerId), eqTo(connectionId), any)).thenReturn(Future(messageId))
    when(connectorIntegration.getConnectionByToken(token)).thenReturn(Future(connection))
    when(repository.mergeSubjectStatus(eqTo(token), any)).thenReturn(Future(1))
    when(repository.findSubjectStatus(token)).thenReturn(Future(Some(subjectStatus)))

    testCode(connectorIntegration, repository, service)
  }

  private def credentialMatcher: Credential = {
    argThat(new ArgumentMatcher[Credential] {
      override def matches(c: Credential): Boolean = {
        c == credential
      }
    })
  }
}
