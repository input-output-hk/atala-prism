package io.iohk.atala.prism.node.poc.estimations

import com.google.protobuf.ByteString
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.identity.{CanonicalPrismDid => Canonical, PrismDid => DID}
import io.iohk.atala.prism.node.NodeConfig
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.poc.estimations.CardanoFeeEstimator.{Estimation, Issuer, TotalEstimation}
import io.iohk.atala.prism.protos.node_internal.AtalaObject
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{node_internal, node_models}
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Estimates the Cardano fees to pay for a given deployment simulation.
  *
  * <p>You can run the estimator with `sbt node/test:run` and choosing `CardanoFeeEstimator` from the list. In order to
  * do so, make sure you have set the proper environment variables, as suggested <a
  * href="https://github.com/input-output-hk/atala/blob/develop/prism-backend/docs/cardano/use-cardano.md">here</a>.
  */
class CardanoFeeEstimator(
    walletId: WalletId,
    paymentAddress: Address,
    cardanoWalletApiClient: CardanoWalletApiClient
) {
  // Max number of credentials that can be issued in the same transaction
  private val MAX_CREDENTIAL_BATCH_SIZE = 2048

  private implicit def patienceConfig: PatienceConfig =
    PatienceConfig(20.seconds, 50.millis)

  def estimate(issuers: List[Issuer]): TotalEstimation = {
    val createDidAtalaObjects = ListBuffer[AtalaObject]()
    val issueCredentialBatchAtalaObjects = ListBuffer[AtalaObject]()
    issuers.foreach { issuer =>
      // Create the DID of the issuer
      val masterKey = EC.generateKeyPair()
      val issuingKey = EC.generateKeyPair()
      val did = createDID(s"Issuer ${issuer.id}")
      val masterKeyOperation = addMasterKeyOperation(masterKey.getPublicKey)
      createDidAtalaObjects += createAtalaObject(
        signOperation(masterKeyOperation, masterKey.getPrivateKey),
        signOperation(
          addIssuingKeyOperation(
            did,
            issuingKey.getPublicKey,
            masterKeyOperation
          ),
          masterKey.getPrivateKey
        )
      )

      // Issue credentials
      issuer.credentialsToIssue.foreach { credentialsToIssue =>
        val batches = math
          .ceil(credentialsToIssue / MAX_CREDENTIAL_BATCH_SIZE.toDouble)
          .toInt
        for (batchId <- 0 until batches) {
          val merkleRoot = new MerkleRoot(
            Sha256.compute(s"Issuer ${issuer.id}, batch $batchId".getBytes)
          )
          issueCredentialBatchAtalaObjects += createAtalaObject(
            signOperation(
              issueCredentialBatchOperation(merkleRoot, did),
              issuingKey.getPrivateKey
            )
          )
        }
      }
    }

    TotalEstimation(
      didCreation = Estimation(
        transactions = createDidAtalaObjects.size,
        fees = estimateFees(createDidAtalaObjects)
      ),
      credentialIssuing = Estimation(
        transactions = issueCredentialBatchAtalaObjects.size,
        fees = estimateFees(issueCredentialBatchAtalaObjects)
      )
    )
  }

  private def estimateFees(atalaObjects: Iterable[AtalaObject]): Lovelace = {
    val atalaObjectsBySize = atalaObjects.groupBy(_.toByteArray.length)
    val fees = atalaObjectsBySize.foldLeft(BigInt(0)) { case (sum, (_, atalaObjectsWithSameSize)) =>
      // For performance, use an arbitrary object to estimate all of the objects with the same size, even though they
      // may get different fees
      sum + atalaObjectsWithSameSize.size * estimateFee(
        atalaObjectsWithSameSize.head
      )
    }
    Lovelace(fees)
  }

  private def estimateFee(atalaObject: AtalaObject): Lovelace = {
    val estimatedFee = cardanoWalletApiClient
      .estimateTransactionFee(
        walletId = walletId,
        payments = List(Payment(paymentAddress, Lovelace(1000000))),
        metadata = Some(AtalaObjectMetadata.toTransactionMetadata(atalaObject))
      )
      .value
      .futureValue
      .toOption
      .value

    // We are only interested in the minimum estimated fee, because the maximum is dynamic and wallet-dependent
    estimatedFee.min
  }

  private def createAtalaObject(
      operations: SignedAtalaOperation*
  ): AtalaObject = {
    val block = node_internal.AtalaBlock(operations)
    AtalaObject(blockOperationCount = operations.size).withBlockContent(block)
  }

  private def signOperation(
      atalaOperation: AtalaOperation,
      privateKey: ECPrivateKey
  ): SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = DID.getDEFAULT_MASTER_KEY_ID,
      operation = Some(atalaOperation),
      signature = ByteString.copyFrom(
        EC.signBytes(atalaOperation.toByteArray, privateKey).getData
      )
    )
  }

  private def createDID(id: String): Canonical = {
    DID.buildCanonical(Sha256.compute(id.getBytes))
  }

  private def addMasterKeyOperation(publicKey: ECPublicKey): AtalaOperation = {
    val createDIDOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.CreateDIDOperation.DIDCreationData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = DID.getDEFAULT_MASTER_KEY_ID,
              usage = node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                publicKeyToProto(publicKey)
              )
            )
          )
        )
      )
    )

    node_models.AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOp))
  }

  private def addIssuingKeyOperation(
      did: Canonical,
      publicKey: ECPublicKey,
      lastOperation: AtalaOperation
  ): AtalaOperation = {
    val createDIDOp = node_models.UpdateDIDOperation(
      previousOperationHash = ByteString.copyFrom(Sha256.compute(lastOperation.toByteArray).getValue),
      id = did.getSuffix,
      actions = List(
        node_models.UpdateDIDAction(
          action = node_models.UpdateDIDAction.Action.AddKey(
            node_models.AddKeyAction(
              key = Some(
                node_models.PublicKey(
                  id = s"issuing0",
                  usage = node_models.KeyUsage.ISSUING_KEY,
                  keyData = node_models.PublicKey.KeyData.EcKeyData(
                    publicKeyToProto(publicKey)
                  )
                )
              )
            )
          )
        )
      )
    )

    node_models.AtalaOperation(AtalaOperation.Operation.UpdateDid(createDIDOp))
  }

  private def issueCredentialBatchOperation(
      merkleRoot: MerkleRoot,
      issuerDid: Canonical
  ): AtalaOperation = {
    val issueCredentialOp = node_models.IssueCredentialBatchOperation(
      credentialBatchData = Some(
        node_models.CredentialBatchData(
          issuerDid = issuerDid.getSuffix,
          merkleRoot = ByteString.copyFrom(merkleRoot.getHash.getValue)
        )
      )
    )

    node_models.AtalaOperation(
      AtalaOperation.Operation.IssueCredentialBatch(issueCredentialOp)
    )
  }

  private def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }
}

object CardanoFeeEstimator {
  case class Issuer(id: Int, credentialsToIssue: List[Int])

  sealed trait EstimationFormat {
    val transactions: Int
    val fees: Lovelace

    def toString(indent: String): String = {
      val averageFee = Lovelace(fees / transactions)
      s"""${indent}Transactions: $transactions
         |${indent}Fees: ${fees.asAda} (${fees.asUsd})
         |  ${indent}Average fee: ${averageFee.asLovelace} (${averageFee.asUsd})
         |""".stripMargin
    }
  }

  case class TotalEstimation(
      didCreation: Estimation,
      credentialIssuing: Estimation
  ) extends EstimationFormat {
    override val transactions: Int =
      didCreation.transactions + credentialIssuing.transactions
    override val fees: Lovelace = Lovelace(
      didCreation.fees + credentialIssuing.fees
    )
  }

  case class Estimation(transactions: Int, fees: Lovelace) extends EstimationFormat

  def main(args: Array[String]): Unit = {
    estimateEthiopia()
    // Force termination as a hanging thread seems to exist
    sys.exit(0)
  }

  private def estimateEthiopia(): Unit = {
    /*
      There are:
        - 5M students
        - 700K teachers
        - 4K schools
      Schools will:
        - Issue IDs to students
        - Report 4 times per year
        - Issue a yearly certificate
      The National Exam Certificate body will:
        - Issue certificates for ~500K students per year (for grades 7, 9, and 11)

      Assumptions:
        - Students will use unpublished DIDs
        - Teachers do not need public DIDs as they don't issue credentials (the school does)
        - School DIDs are batched as part of the deployment
        - Students are evenly distributed in schools
     */
    val students = 5000000
    val schools = 4000
    val yearlyNationalExamCertificates = 500000
    val studentsPerSchool = students / schools
    val yearlyReportsPerStudent = 4
    val yearlyCertificatesPerStudent = 1
    val yearlyCredentialsPerStudent =
      yearlyReportsPerStudent + yearlyCertificatesPerStudent

    // Issue `yearlyNationalExamCertificates` credentials once per year
    val nationalExamCertBody =
      Issuer(id = 0, credentialsToIssue = List(yearlyNationalExamCertificates))
    val schoolIssuers = List.tabulate(schools)(schoolId =>
      // Issue `studentsPerSchool` credentials `yearlyCredentialsPerStudent` times in a year
      Issuer(
        id = schoolId,
        credentialsToIssue = List.fill(yearlyCredentialsPerStudent)(studentsPerSchool)
      )
    )

    val estimator = createCardanoFeeEstimator()
    val estimation =
      estimator.estimate(List(nationalExamCertBody) ++ schoolIssuers)

    println(s"""Ethiopia estimation:
         |  Initial setup (DID creation):
         |${estimation.didCreation.toString("    - ")}
         |  Yearly (credential issuing):
         |${estimation.credentialIssuing.toString("    - ")}
         |  Total:
         |${estimation.toString("    - ")}
         |""".stripMargin)
  }

  private def createCardanoFeeEstimator(): CardanoFeeEstimator = {
    implicit val ec: ExecutionContext = ExecutionContext.global

    val clientConfig =
      NodeConfig.cardanoConfig(ConfigFactory.load().getConfig("cardano"))
    val walletId = WalletId.from(clientConfig.walletId).value
    val paymentAddress = Address(clientConfig.paymentAddress)
    val cardanoWalletApiClient = CardanoWalletApiClient(
      clientConfig.cardanoClientConfig.cardanoWalletConfig
    )

    new CardanoFeeEstimator(walletId, paymentAddress, cardanoWalletApiClient)
  }

  implicit class LovelaceFormat(val amount: Lovelace) {
    private val ADA_USD_PRICE = 0.103377

    private def toAda: Double = {
      amount.toDouble / 1000000
    }

    def asLovelace: String = {
      f"$amount lovelace"
    }

    def asAda: String = {
      f"$toAda%.6f ADA"
    }

    def asUsd: String = {
      f"$$${toAda * ADA_USD_PRICE}%.2f USD"
    }
  }
}
