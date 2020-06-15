package io.iohk.node.services

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.utils.FutureEither
import io.iohk.node.errors.NodeError
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.{CredentialId, DIDSuffix}
import io.iohk.node.models.nodeState.CredentialState
import io.iohk.node.operations.TimestampInfo
import io.iohk.node.repositories.CredentialsRepository
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.EitherValues._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class CredentialsServiceSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val credentialsRepository = mock[CredentialsRepository]
  lazy val credentialsService = new CredentialsService(credentialsRepository)

  "CredentialsService.getCredentialState" should {
    "fail when credentialId is not present in the database" in {
      val credentialId = CredentialId(SHA256Digest.compute("testId".getBytes()))

      val error = UnknownValueError("credential_id", credentialId.id)
      val failure = new FutureEither[NodeError, CredentialState](
        Future(
          Left(error)
        )
      )

      doReturn(failure).when(credentialsRepository).find(credentialId)

      val result = credentialsService.getCredentialState(credentialId).value.futureValue.left.value
      result must be(error)
    }

    "return proper data when found in the database" in {
      val credentialId = CredentialId(SHA256Digest.compute("testId".getBytes()))

      val credState =
        CredentialState(
          contentHash = SHA256Digest.compute("content".getBytes()),
          credentialId = credentialId,
          issuerDIDSuffix = DIDSuffix(SHA256Digest.compute("testDID".getBytes())),
          issuedOn = TimestampInfo.dummyTime,
          revokedOn = None,
          lastOperation = SHA256Digest.compute("lastOp".getBytes())
        )

      val success = new FutureEither[NodeError, CredentialState](
        Future(
          Right(credState)
        )
      )

      doReturn(success).when(credentialsRepository).find(credentialId)

      val result = credentialsService.getCredentialState(credentialId).value.futureValue.right.value
      result must be(credState)
    }
  }
}
