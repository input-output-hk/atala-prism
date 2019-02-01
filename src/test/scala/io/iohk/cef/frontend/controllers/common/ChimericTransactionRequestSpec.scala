package io.iohk.cef.frontend.controllers.common

import io.iohk.codecs.nio.auto._
import io.iohk.crypto._
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  CreateNonSignableChimericTransactionFragment,
  CreateSignableChimericTransactionFragment
}
import io.iohk.cef.ledger.chimeric._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class ChimericTransactionRequestSpec extends WordSpec with MustMatchers {

  import Codecs._

  val signingKeyPair = generateSigningKeyPair()
  val signingKeyPair2 = generateSigningKeyPair()

  "chimericTransactionRequestJsonFormat" should {
    "be able to serialize and deserialize a transaction including all fragment types" in {
      val fragments = List(
        CreateSignableChimericTransactionFragment(
          Withdrawal(address = signingKeyPair.public, value = Value(Map("MXN" -> BigDecimal(10))), nonce = 1),
          signingKeyPair.`private`
        ),
        CreateNonSignableChimericTransactionFragment(Mint(value = Value(Map("USD" -> BigDecimal(200))))),
        CreateSignableChimericTransactionFragment(
          Input(txOutRef = TxOutRef("txid", 0), value = Value(Map("CAD" -> BigDecimal(200)))),
          signingKeyPair.`private`
        ),
        CreateNonSignableChimericTransactionFragment(Fee(value = Value(Map("GBP" -> BigDecimal(9990))))),
        CreateNonSignableChimericTransactionFragment(Output(Value(Map("EUR" -> BigDecimal(5))), signingKeyPair.public)),
        CreateNonSignableChimericTransactionFragment(
          Deposit(address = signingKeyPair2.public, value = Value(Map("PLN" -> BigDecimal(100000))))
        ),
        CreateNonSignableChimericTransactionFragment(CreateCurrency(currency = "AUD")),
        CreateNonSignableChimericTransactionFragment(SignatureTxFragment(sign("foo", signingKeyPair.`private`)))
      )
      val request = CreateChimericTransactionRequest(fragments, "1")
      val serialized = Json.toJson(request)
      val deserialized = serialized.as[CreateChimericTransactionRequest]

      deserialized mustEqual request
    }
  }
}
