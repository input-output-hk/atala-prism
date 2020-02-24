package io.iohk.cvp.intdemo

import io.iohk.connector.ConnectorService
import org.scalatest.FlatSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import IDServiceImplSpec._
import io.grpc.stub.StreamObserver
import io.iohk.cvp.connector.protos.{
  Connection,
  GenerateConnectionTokenRequest,
  GenerateConnectionTokenResponse,
  GetConnectionByTokenRequest,
  GetConnectionByTokenResponse
}
import io.iohk.cvp.intdemo.protos.{
  GetConnectionTokenRequest,
  GetSubjectStatusRequest,
  GetSubjectStatusResponse,
  SubjectStatus
}
import org.mockito.MockitoSugar._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.concurrent.ScalaFutures.PatienceConfig
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually.eventually
import org.mockito.ArgumentMatchersSugar.{any, eqTo}

class IDServiceImplSpec extends FlatSpec {

  implicit val pc: PatienceConfig = PatienceConfig(1 second, 100 millis)

  "getConnectionToken" should "create a connection token via the connector" in idService {
    (connectorService, credentialStatusRepository, idService) =>
      when(connectorService.generateConnectionToken(GenerateConnectionTokenRequest()))
        .thenReturn(Future(GenerateConnectionTokenResponse(token)))
      when(credentialStatusRepository.merge(token, SubjectStatus.UNCONNECTED.value)).thenReturn(Future(1))

      val tokenResponse = idService.getConnectionToken(GetConnectionTokenRequest()).futureValue

      tokenResponse.connectionToken shouldBe token
  }

  "getSubjectStatus" should "obtain connection status when one is available" in idService {
    (_, credentialStatusRepository, idService) =>
      val expectedStatus = SubjectStatus.CONNECTED
      when(credentialStatusRepository.find(token)).thenReturn(Future(Some(expectedStatus.value)))

      val statusResponse = idService.getSubjectStatus(GetSubjectStatusRequest(token)).futureValue

      statusResponse.subjectStatus shouldBe expectedStatus
  }

  it should "return UNCONNECTED when invoked with an invalid/random token" in idService {
    (_, credentialStatusRepository, idService) =>
      val expectedStatus = SubjectStatus.UNCONNECTED
      when(credentialStatusRepository.find(token)).thenReturn(Future(None))

      val statusResponse = idService.getSubjectStatus(GetSubjectStatusRequest(token)).futureValue

      statusResponse.subjectStatus shouldBe expectedStatus
  }

  "getSubjectStatusStream" should "update the subject status when the connection is accepted by the wallet" in idService {
    (connectorService, credentialStatusRepository, idService) =>
      val streamObserver = mock[StreamObserver[GetSubjectStatusResponse]]
      when(credentialStatusRepository.find(token))
        .thenAnswer(Future(Some(SubjectStatus.UNCONNECTED.value)))
        .andThenAnswer(Future(Some(SubjectStatus.CONNECTED.value)))
      when(credentialStatusRepository.merge(eqTo(token), any)).thenReturn(Future(1))
      when(connectorService.getConnectionByToken(GetConnectionByTokenRequest(token)))
        .thenReturn(Future(GetConnectionByTokenResponse(Some(Connection(token)))))

      idService.getSubjectStatusStream(GetSubjectStatusRequest(token), streamObserver)

      eventually {
        verify(credentialStatusRepository).merge(token, SubjectStatus.CONNECTED.value)
        verify(streamObserver, atLeastOnce).onNext(GetSubjectStatusResponse(SubjectStatus.CONNECTED))
      }
  }
}

object IDServiceImplSpec {
  implicit val ec: ExecutionContext = ExecutionContext.global
  val token = "a token"

  def idService(testCode: (ConnectorService, CredentialStatusRepository, IDServiceImpl) => Any): Unit = {
    val connectorService = mock[ConnectorService]
    val credentialStatusRepository = mock[CredentialStatusRepository]
    val service = new IDServiceImpl(connectorService, schedulerPeriod = 1 milli)(credentialStatusRepository)

    testCode(connectorService, credentialStatusRepository, service)
  }
}
