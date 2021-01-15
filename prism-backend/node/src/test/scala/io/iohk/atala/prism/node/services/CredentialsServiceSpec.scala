package io.iohk.atala.prism.node.services

import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.repositories.CredentialsRepository
import io.iohk.atala.prism.utils.FutureEither
import org.mockito.scalatest.MockitoSugar
import org.scalatest.EitherValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CredentialsServiceSpec extends AnyWordSpec with must.Matchers with ScalaFutures with MockitoSugar {
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
      val dummyTimestampInfo = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)

      val credState =
        CredentialState(
          contentHash = SHA256Digest.compute("content".getBytes()),
          credentialId = credentialId,
          issuerDIDSuffix = DIDSuffix.unsafeFromDigest(SHA256Digest.compute("testDID".getBytes())),
          issuedOn = dummyTimestampInfo,
          revokedOn = None,
          lastOperation = SHA256Digest.compute("lastOp".getBytes())
        )

      val success = new FutureEither[NodeError, CredentialState](
        Future(
          Right(credState)
        )
      )

      doReturn(success).when(credentialsRepository).find(credentialId)

      credentialsService.getCredentialState(credentialId).value.futureValue must beRight(credState)
    }
  }
}
