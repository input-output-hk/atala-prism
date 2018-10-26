package io.iohk.cef.frontend.models

import io.iohk.cef.LedgerId
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.chimeric._
import spray.json._

trait ExtraJsonFormats extends ChimericTxFragmentFormat {

  protected case class HasSigningPrivateKey(signingPrivateKey: SigningPrivateKey)
  protected implicit val HasSigningPrivateKeyJsonFormat: RootJsonFormat[HasSigningPrivateKey] =
    jsonFormat1(HasSigningPrivateKey.apply)

}

/**
  * {{{
  *
  *   # Some setup
  *   >>> import spray.json._
  *   >>> import io.iohk.cef.ledger.chimeric._
  *   >>> import io.iohk.cef.crypto._
  *   >>> val SigningKeyPair(_, priv) = generateSigningKeyPair
  *
  *   # Convert a non signable fragment into json
  *   >>> val ntx: CreateChimericTransactionFragment = CreateNonSignableChimericTransactionFragment(Mint(Value(Map("USD" -> BigDecimal(123)))))
  *   >>> ntx.toJson
  *   {"value":{"USD":123},"type":"Mint"}
  *
  *   # Recover a non signable fragment from a json
  *   >>> """{"value":{"USD":123},"type":"Mint"}""".parseJson.convertTo[CreateChimericTransactionFragment]
  *   CreateNonSignableChimericTransactionFragment(Mint(Value(Map(USD -> 123))))
  *
  *   # Round trip for a signable fragment
  *   >>> val stx: CreateChimericTransactionFragment = {
  *   ...   CreateSignableChimericTransactionFragment(Withdrawal("Address1", Value(Map("USD" -> BigDecimal(123))), 999), priv)}
  *   >>> stx.toJson.convertTo[CreateChimericTransactionFragment] == stx
  *   true
  * }}}
  */
sealed trait CreateChimericTransactionFragment {
  val fragment: ChimericTxFragment

  def `type`: ChimericTransactionFragmentType
}

object CreateChimericTransactionFragment extends ExtraJsonFormats {

  implicit val createChimericTransactionFragmentJsonFormat: JsonFormat[CreateChimericTransactionFragment] =
    new JsonFormat[CreateChimericTransactionFragment] {
      def read(json: JsValue): CreateChimericTransactionFragment = {
        json.convertTo[ChimericTxFragment] match {
          case s: SignableChimericTxFragment =>
            val key = json.convertTo[HasSigningPrivateKey].signingPrivateKey
            CreateSignableChimericTransactionFragment(s, key)
          case n: NonSignableChimericTxFragment =>
            CreateNonSignableChimericTransactionFragment(n)
        }
      }

      def write(obj: CreateChimericTransactionFragment): JsValue = {
        val part = obj.fragment.toJson
        obj match {
          case n: CreateNonSignableChimericTransactionFragment =>
            part
          case s: CreateSignableChimericTransactionFragment =>
            val part2 = HasSigningPrivateKey(s.signingPrivateKey)
            JsObject(part.asJsObject.fields ++ part2.toJson.asJsObject.fields)
        }
      }
    }
}

case class CreateNonSignableChimericTransactionFragment(override val fragment: NonSignableChimericTxFragment)
    extends CreateChimericTransactionFragment {
  override def `type`: NonSignableChimericTransactionFragmentType =
    NonSignableChimericTransactionFragmentType.of(fragment)
}

case class CreateSignableChimericTransactionFragment(
    override val fragment: SignableChimericTxFragment,
    signingPrivateKey: SigningPrivateKey)
    extends CreateChimericTransactionFragment {
  override def `type`: SignableChimericTransactionFragmentType =
    SignableChimericTransactionFragmentType.of(fragment)
}

/**
  * {{{
  *
  *   # Some setup
  *   >>> import spray.json._
  *   >>> import io.iohk.cef.ledger.chimeric._
  *
  *   # Convert a value into json
  *   >>> val tx: CreateChimericTransactionRequest = CreateChimericTransactionRequest(Seq(CreateNonSignableChimericTransactionFragment(Mint(Value(Map("USD" -> BigDecimal(123)))))), "890")
  *   >>> tx.toJson
  *   {"fragments":[{"value":{"USD":123},"type":"Mint"}],"ledgerId":"890"}
  *
  *   # Recover a value from a json
  *   >>> """{"fragments":[{"value":{"USD":123},"type":"Mint"}],"ledgerId":"890"}""".parseJson.convertTo[CreateChimericTransactionRequest]
  *   CreateChimericTransactionRequest(List(CreateNonSignableChimericTransactionFragment(Mint(Value(Map(USD -> 123))))),890)
  * }}}
  */
case class CreateChimericTransactionRequest(fragments: Seq[CreateChimericTransactionFragment], ledgerId: LedgerId)

object CreateChimericTransactionRequest extends ExtraJsonFormats {

  implicit val createChimericTransactionRequestJsonFormat: RootJsonFormat[CreateChimericTransactionRequest] =
    jsonFormat2(CreateChimericTransactionRequest.apply)

}

case class SubmitChimericTransactionFragment(fragment: ChimericTxFragment) {
  def `type`: ChimericTransactionFragmentType =
    ChimericTransactionFragmentType.of(fragment)
}
case class SubmitChimericTransactionRequest(fragments: Seq[SubmitChimericTransactionFragment], ledgerId: LedgerId)

object ChimericTransactionRequest extends ExtraJsonFormats {
  implicit val submitChimericTransactionFragmentJsonFormat: JsonFormat[SubmitChimericTransactionFragment] =
    new JsonFormat[SubmitChimericTransactionFragment] {
      def read(json: JsValue): SubmitChimericTransactionFragment =
        SubmitChimericTransactionFragment(json.convertTo[ChimericTxFragment])

      def write(obj: SubmitChimericTransactionFragment): JsValue =
        obj.fragment.toJson
    }

  implicit val submitChimericTransactionRequestJsonFormat: RootJsonFormat[SubmitChimericTransactionRequest] =
    jsonFormat2(SubmitChimericTransactionRequest.apply)
}
