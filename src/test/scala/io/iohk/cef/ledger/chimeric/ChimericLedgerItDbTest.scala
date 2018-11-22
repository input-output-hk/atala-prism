package io.iohk.cef.ledger.chimeric

import java.time.Clock

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.chimeric.SignatureTxFragment.signFragments
import io.iohk.cef.ledger.storage.scalike.LedgerStateStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerFixture, LedgerState}
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio.auto._

trait ChimericLedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with LedgerFixture
    with EitherValues {

  def createLedger(ledgerStateStorageDao: LedgerStateStorageDao)(implicit dBSession: DBSession): Ledger = {
    val ledgerStateStorage = new LedgerStateStorageImpl("chimericLedger", ledgerStateStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    val ledgerStorageDao = new LedgerStorageDao(Clock.systemUTC())
    val ledgerStorage = new LedgerStorageImpl(ledgerStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    createLedger(ledgerStateStorage, ledgerStorage)
  }

  val signingKeyPair = generateSigningKeyPair()
  val signingPublicKey = signingKeyPair.public
  val signingPrivateKey = signingKeyPair.`private`

  behavior of "ChimericLedger"

  it should "store transactions" in { implicit s =>
    val stateStorage = new LedgerStateStorageDao

    def slice(keys: Set[String]): LedgerState[ChimericStateResult] =
      stateStorage.slice[ChimericStateResult]("chimericLedger", keys)

    val address1 = "address1"
    val address2 = "address2"
    val currency1 = "currency1"
    val currency2 = "currency2"
    val value1 = Value(Map(currency1 -> BigDecimal(10), currency2 -> BigDecimal(20)))
    val value2 = Value(Map(currency1 -> BigDecimal(100), currency2 -> BigDecimal(200)))
    val value3 = Value(Map(currency1 -> BigDecimal(2)))
    val singleFee = Value(Map(currency1 -> BigDecimal(1)))
    val multiFee = Value(Map(currency1 -> BigDecimal(1), currency2 -> BigDecimal(2)))
    val ledger = createLedger(stateStorage)
    val utxoTx = ChimericTx(
      signFragments(
        Seq(
          Withdrawal(address1, value3, nonce = 2),
          Output(value3 - singleFee, signingPublicKey),
          Fee(singleFee)
        ),
        signingPrivateKey))
    val transactions = List[ChimericTx](
      ChimericTx(
        Seq(
          CreateCurrency(currency1),
          CreateCurrency(currency2),
          Mint(value1),
          Mint(value2),
          Deposit(address1, value1 + value2, signingPublicKey)
        )),
      ChimericTx(
        signFragments(
          Seq(
            Withdrawal(address1, value1, 1),
            Deposit(address2, value1 - multiFee, signingPublicKey),
            Fee(multiFee)
          ),
          signingPrivateKey)),
      utxoTx
    )
    val header = BlockHeader()
    val block = Block[ChimericStateResult, ChimericTx](header, transactions)
    val result = ledger(block)
    result.isRight mustBe true

    val address1Key = ChimericLedgerState.getAddressPartitionId(address1)
    val address2Key = ChimericLedgerState.getAddressPartitionId(address2)
    val currency1Key = ChimericLedgerState.getCurrencyPartitionId(currency1)
    val currency2Key = ChimericLedgerState.getCurrencyPartitionId(currency2)
    val utxoKey = ChimericLedgerState.getUtxoPartitionId(TxOutRef(utxoTx.txId, 1))
    val allKeys = Set(address1Key, address2Key, currency1Key, currency2Key, utxoKey)
    slice(allKeys) mustBe LedgerState[ChimericStateResult](
      Map(
        currency1Key -> CreateCurrencyResult(CreateCurrency(currency1)),
        currency2Key -> CreateCurrencyResult(CreateCurrency(currency2)),
        utxoKey -> UtxoResult(value3 - singleFee, Some(signingPublicKey)),
        address1Key -> AddressResult(value2 - value3, Some(signingPublicKey)),
        address2Key -> AddressResult(value1 - multiFee, Some(signingPublicKey))
      ))

    val txFragments: Seq[ChimericTxFragment] = Seq(
      Input(TxOutRef(utxoTx.txId, 1), value3 - singleFee),
      Fee(value3 - singleFee),
      Withdrawal(address1, value2 - value3, 3),
      Fee(value2 - value3)
    )

    val block2 =
      Block[ChimericStateResult, ChimericTx](
        header,
        Seq(ChimericTx(signFragments(signFragments(txFragments, signingPrivateKey), signingPrivateKey))))

    val result2 = ledger(block2)
    result2.isRight mustBe true
    slice(allKeys) mustBe LedgerState[ChimericStateResult](
      Map(
        currency1Key -> CreateCurrencyResult(CreateCurrency(currency1)),
        currency2Key -> CreateCurrencyResult(CreateCurrency(currency2)),
        address2Key -> AddressResult(value1 - multiFee, Some(signingPublicKey))
      ))
  }
}
