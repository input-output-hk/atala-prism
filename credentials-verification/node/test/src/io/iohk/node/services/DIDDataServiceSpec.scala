package io.iohk.node.services

import doobie.implicits._
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.operations.{CreateDIDOperation, CreateDIDOperationSpec}
import io.iohk.node.repositories.DIDDataRepository
import org.scalatest.EitherValues._

import scala.concurrent.duration._

class DIDDataServiceSpec extends PostgresRepositorySpec {
  import CreateDIDOperationSpec._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)
  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val didDataService = new DIDDataService(didDataRepository)
  override val tables = List("public_keys", "did_data")

  "DIDDataServiceSpec.findByDID" should {
    "retrieve DID document from database" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation).right.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val did = s"did:atala:${parsedOperation.id.suffix}"
      didDataService.findByDID(did).value.futureValue mustBe a[Right[_, _]]
    }

    "return error when did is in invalid format" in {
      val did = s"did:atala:1111111111111111"
      didDataService.findByDID(did).value.futureValue mustBe a[Left[UnknownValueError, _]]
    }

  }

}
