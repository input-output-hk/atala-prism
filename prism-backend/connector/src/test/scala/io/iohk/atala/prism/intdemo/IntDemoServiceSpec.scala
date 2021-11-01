package io.iohk.atala.prism.intdemo

import cats.syntax.functor._
import io.grpc.stub.StreamObserver
import io.iohk.atala.prism.connector.model.{Connection, ConnectionId, MessageId, TokenString}
import IntDemoServiceSpec._
import Testing.{eventually, neverEver}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.intdemo.protos.{intdemo_api, intdemo_models}
import io.iohk.atala.prism.protos.credential_models
import org.mockito.ArgumentMatchersSugar.{any, argThat, eqTo}
import org.mockito.MockitoSugar.{after, mock, verify, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture}
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class IntDemoServiceSpec extends AnyFlatSpec {

  implicit val pc: PatienceConfig = PatienceConfig(1 second, 100 millis)

  "getConnectionToken" should "create a connection token via the connector" in intDemoService {
    (connectorIntegration, repository, intDemoService) =>
      when(connectorIntegration.generateConnectionToken(issuerId))
        .thenReturn(Future(token))
      when(
        repository.mergeSubjectStatus(
          token,
          intdemo_models.SubjectStatus.UNCONNECTED
        )
      ).thenReturn(Future(1))

      val tokenResponse = intDemoService
        .getConnectionToken(intdemo_api.GetConnectionTokenRequest())
        .futureValue

      tokenResponse.connectionToken shouldBe token.token
  }

  val nonEmittingStates = Table(
    ("Current status", "Connection", "User Info", "Expected response"),
    (
      intdemo_models.SubjectStatus.UNCONNECTED,
      None,
      None,
      intdemo_models.SubjectStatus.UNCONNECTED
    ),
    (
      intdemo_models.SubjectStatus.UNCONNECTED,
      Some(connection),
      None,
      intdemo_models.SubjectStatus.CONNECTED
    ),
    (
      intdemo_models.SubjectStatus.UNCONNECTED,
      None,
      Some(userInfo),
      intdemo_models.SubjectStatus.UNCONNECTED
    ),
    (
      intdemo_models.SubjectStatus.CONNECTED,
      Some(connection),
      None,
      intdemo_models.SubjectStatus.CONNECTED
    ),
    (
      intdemo_models.SubjectStatus.CREDENTIAL_SENT,
      Some(connection),
      Some(userInfo),
      intdemo_models.SubjectStatus.CREDENTIAL_SENT
    )
  )

  forAll(nonEmittingStates) { (currentStatus, connection, userInfo, expectedResponse) =>
    {
      "getSubjectStatusStream" should s"return $expectedResponse given " +
        s"current status is $currentStatus, " +
        s"wallet is connected is ${connection.isDefined} and " +
        s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
          currentStatus,
          connection,
          userInfo
        ) { (connectorIntegration, _, intDemoService) =>
          val streamObserver =
            mock[StreamObserver[intdemo_api.GetSubjectStatusResponse]]
          intDemoService.getSubjectStatusStream(
            intdemo_api.GetSubjectStatusRequest(token.token),
            streamObserver
          )
          verify(streamObserver, eventually.atLeastOnce())
            .onNext(intdemo_api.GetSubjectStatusResponse(expectedResponse))
          verify(connectorIntegration, neverEver).sendCredential(
            any[ParticipantId],
            any[ConnectionId],
            any[credential_models.PlainTextCredential]
          )
          verify(streamObserver, neverEver).onError(any)
        }
    }

    "getSubjectStatus" should s"return $expectedResponse given " +
      s"current status is $currentStatus, " +
      s"wallet is connected is ${connection.isDefined} and " +
      s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
        currentStatus,
        connection,
        userInfo
      ) { (connectorIntegration, _, intDemoService) =>
        val status = intDemoService.getSubjectStatus(
          intdemo_api.GetSubjectStatusRequest(token.token)
        )

        status.futureValue shouldBe intdemo_api.GetSubjectStatusResponse(
          expectedResponse
        )
        verify(connectorIntegration, neverEver).sendCredential(
          any[ParticipantId],
          any[ConnectionId],
          any[credential_models.PlainTextCredential]
        )
      }
  }

  val emittingStates = Table(
    ("Current status", "Connection", "User Info", "Expected response"),
    (
      intdemo_models.SubjectStatus.UNCONNECTED,
      Some(connection),
      Some(userInfo),
      intdemo_models.SubjectStatus.CREDENTIAL_SENT
    ),
    (
      intdemo_models.SubjectStatus.CONNECTED,
      Some(connection),
      Some(userInfo),
      intdemo_models.SubjectStatus.CREDENTIAL_SENT
    )
  )

  forAll(emittingStates) { (currentStatus, connection, userInfo, expectedResponse) =>
    {
      "getSubjectStatusStream" should s"emit credential and return $expectedResponse given " +
        s"current status is $currentStatus, " +
        s"wallet is connected is ${connection.isDefined} and " +
        s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
          currentStatus,
          connection,
          userInfo
        ) { (connectorIntegration, _, intDemoService) =>
          val streamObserver =
            mock[StreamObserver[intdemo_api.GetSubjectStatusResponse]]
          intDemoService.getSubjectStatusStream(
            intdemo_api.GetSubjectStatusRequest(token.token),
            streamObserver
          )

          verify(connectorIntegration, eventually.atLeastOnce())
            .sendCredential(
              eqTo(issuerId),
              eqTo(connectionId),
              credentialMatcher
            )
          verify(streamObserver, eventually.atLeastOnce()).onCompleted()
          verify(streamObserver, neverEver).onError(any)
        }

      "getSubjectStatus" should s"emit credential and return $expectedResponse given " +
        s"current status is $currentStatus, " +
        s"wallet is connected is ${connection.isDefined} and " +
        s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
          currentStatus,
          connection,
          userInfo
        ) { (connectorIntegration, _, intDemoService) =>
          val status = intDemoService.getSubjectStatus(
            intdemo_api.GetSubjectStatusRequest(token.token)
          )

          status.futureValue shouldBe intdemo_api.GetSubjectStatusResponse(
            expectedResponse
          )
          verify(connectorIntegration, eventually.times(1))
            .sendCredential(
              eqTo(issuerId),
              eqTo(connectionId),
              credentialMatcher
            )
        }
    }
  }

  val illegalStates = Table(
    ("Current status", "Connection", "User Info"),
    (intdemo_models.SubjectStatus.CONNECTED, None, None),
    (intdemo_models.SubjectStatus.CONNECTED, None, Some(userInfo)),
    (intdemo_models.SubjectStatus.CREDENTIAL_SENT, None, None),
    (intdemo_models.SubjectStatus.CREDENTIAL_SENT, None, Some(userInfo)),
    (intdemo_models.SubjectStatus.CREDENTIAL_SENT, Some(connection), None)
  )

  forAll(illegalStates) { (currentStatus, connection, userInfo) =>
    {
      "getSubjectStatusStream" should s"report an error given " +
        s"current status is $currentStatus, " +
        s"wallet is connected is ${connection.isDefined} and " +
        s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
          currentStatus,
          connection,
          userInfo
        ) { (connectorIntegration, _, intDemoService) =>
          val streamObserver =
            mock[StreamObserver[intdemo_api.GetSubjectStatusResponse]]
          intDemoService.getSubjectStatusStream(
            intdemo_api.GetSubjectStatusRequest(token.token),
            streamObserver
          )

          verify(connectorIntegration, neverEver).sendCredential(
            any[ParticipantId],
            any[ConnectionId],
            any[credential_models.PlainTextCredential]
          )
          verify(streamObserver, eventually.times(1))
            .onError(any[IllegalStateException])
        }

      "getSubjectStatus" should s"report an error given " +
        s"current status is $currentStatus, " +
        s"wallet is connected is ${connection.isDefined} and " +
        s"user info is uploaded is ${userInfo.isDefined}" in intDemoService(
          currentStatus,
          connection,
          userInfo
        ) { (connectorIntegration, _, intDemoService) =>
          val status = intDemoService.getSubjectStatus(
            intdemo_api.GetSubjectStatusRequest(token.token)
          )

          status.failed.futureValue
            .isInstanceOf[IllegalStateException] shouldBe true
          verify(connectorIntegration, neverEver)
            .sendCredential(
              any[ParticipantId],
              any[ConnectionId],
              any[credential_models.PlainTextCredential]
            )
        }
    }
  }

  "getSubjectStatusStream" should "handle callback errors by terminating" in intDemoService { (_, _, intDemoService) =>
    val streamObserver =
      mock[StreamObserver[intdemo_api.GetSubjectStatusResponse]]

    when(streamObserver.onNext(any[intdemo_api.GetSubjectStatusResponse]))
      .thenThrow(new RuntimeException("timeout or something"))

    intDemoService.getSubjectStatusStream(
      intdemo_api.GetSubjectStatusRequest(token.token),
      streamObserver
    )

    verify(streamObserver, after(100).atMost(1))
      .onNext(any[intdemo_api.GetSubjectStatusResponse])
  }

  "getSubjectStatusStream" should s"emit proof requests return CONNECTED when a user connects" in intDemoService(
    intdemo_models.SubjectStatus.UNCONNECTED,
    Some(connection),
    None
  ) { (connectorIntegration, _, intDemoService) =>
    val streamObserver =
      mock[StreamObserver[intdemo_api.GetSubjectStatusResponse]]
    intDemoService.getSubjectStatusStream(
      intdemo_api.GetSubjectStatusRequest(token.token),
      streamObserver
    )

    verify(connectorIntegration, eventually.atLeastOnce())
      .sendProofRequest(eqTo(issuerId), eqTo(connectionId), proofRequestMatcher)
    verify(streamObserver, eventually.atLeastOnce())
      .onNext(
        intdemo_api.GetSubjectStatusResponse(
          intdemo_models.SubjectStatus.CONNECTED
        )
      )
    verify(streamObserver, neverEver).onError(any)
  }

  "getSubjectStatus" should s"emit proof requests return CONNECTED when a user connects" in intDemoService(
    intdemo_models.SubjectStatus.UNCONNECTED,
    Some(connection),
    None
  ) { (connectorIntegration, _, intDemoService) =>
    val status = intDemoService.getSubjectStatus(
      intdemo_api.GetSubjectStatusRequest(token.token)
    )

    status.futureValue shouldBe intdemo_api.GetSubjectStatusResponse(
      intdemo_models.SubjectStatus.CONNECTED
    )
    verify(connectorIntegration, eventually.times(1))
      .sendProofRequest(eqTo(issuerId), eqTo(connectionId), proofRequestMatcher)
  }
}

object IntDemoServiceSpec {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val connectionId = ConnectionId.random()
  private val messageId = MessageId.random()
  private val issuerId = IdServiceImpl.issuerId
  private val token = new TokenString("a token")
  private val connection =
    Connection(connectionToken = token, connectionId = connectionId)
  private val userInfo = "X"
  private val credential = credential_models.PlainTextCredential(
    encodedCredential = "some encoded credential",
    encodedMerkleProof = "encoded proof"
  )
  private val proofRequest = credential_models.ProofRequest(
    typeIds = Seq("type-id"),
    connectionToken = token.token
  )

  def intDemoService(
      testCode: (
          ConnectorIntegration,
          IntDemoRepository,
          IntDemoService[String]
      ) => Any
  ): Unit = {
    intDemoService(intdemo_models.SubjectStatus.UNCONNECTED, None, None)(
      testCode
    )
  }

  def intDemoService(
      subjectStatus: intdemo_models.SubjectStatus,
      connection: Option[Connection],
      requiredData: Option[String]
  )(
      testCode: (
          ConnectorIntegration,
          IntDemoRepository,
          IntDemoService[String]
      ) => Any
  ): Unit = {
    val connectorIntegration = mock[ConnectorIntegration]
    val repository = mock[IntDemoRepository]

    when(
      connectorIntegration.sendCredential(
        eqTo(issuerId),
        eqTo(connectionId),
        any
      )
    ).thenReturn(Future(messageId))
    when(
      connectorIntegration.sendProofRequest(
        eqTo(issuerId),
        eqTo(connectionId),
        any
      )
    ).thenReturn(Future(messageId))
    when(connectorIntegration.getConnectionByToken(token))
      .thenReturn(Future(connection))
    when(repository.mergeSubjectStatus(eqTo(token), any)).thenReturn(Future(1))
    when(repository.findSubjectStatus(token))
      .thenReturn(Future(Some(subjectStatus)))

    val proofRequestIssuer: Connection => Future[Unit] = { connection =>
      connectorIntegration
        .sendProofRequest(issuerId, connection.connectionId, proofRequest)
        .void
    }

    val service = new IntDemoService[String](
      issuerId = issuerId,
      connectorIntegration = connectorIntegration,
      intDemoRepository = repository,
      schedulerPeriod = 1 second,
      requiredDataLoader = _ => Future(requiredData),
      proofRequestIssuer = proofRequestIssuer,
      getCredential = _ => credential
    )

    testCode(connectorIntegration, repository, service)
    ()
  }

  private def credentialMatcher: credential_models.PlainTextCredential = {
    argThat { c: credential_models.PlainTextCredential =>
      c == credential
    }
  }

  private def proofRequestMatcher: credential_models.ProofRequest = {
    argThat { p: credential_models.ProofRequest =>
      p == proofRequest
    }
  }
}
