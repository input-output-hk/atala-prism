package io.iohk.cef.frontend.models

import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.crypto._
import org.scalatest.{MustMatchers, WordSpec}
import spray.json._

class ChimericTransactionRequestSpec extends WordSpec with MustMatchers {

  val signingKeyPair = generateSigningKeyPair()

  "chimericTransactionRequestJsonFormat" should {
    "be able to serialize and deserialize a transaction including all fragment types" in {
      val fragments = List(
        CreateSignableChimericTransactionFragment(
          Withdrawal(address = "dummy", value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair.`private`),
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair.`private`),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair.public)),
        CreateNonSignableChimericTransactionFragment(
          Deposit(address = "another", value = Value(Map("PLN" -> BigDecimal(100000))), signingKeyPair.public)),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD")),
        CreateNonSignableChimericTransactionFragment(SignatureTxFragment(sign("foo", signingKeyPair.`private`)))
      )
      val request = CreateChimericTransactionRequest(fragments, 1)
      val serialized = request.toJson
      val deserialized = serialized.convertTo[CreateChimericTransactionRequest]

      deserialized mustEqual request
    }
  }
}
