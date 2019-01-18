package io.iohk.cef.frontend.models

import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.chimeric._
import play.api.libs.json._

trait ExtraJsonFormats extends ChimericTxFragmentFormat {

  protected case class HasSigningPrivateKey(signingPrivateKey: SigningPrivateKey)
  protected implicit val HasSigningPrivateKeyJsonFormat: OFormat[HasSigningPrivateKey] = Json.format

  protected case class CouldHaveSigningPrivateKey(signingPrivateKey: Option[SigningPrivateKey])
  protected implicit val CouldHaveSigningPrivateKeyJsonFormat: OFormat[CouldHaveSigningPrivateKey] = Json.format

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

  implicit val createChimericTransactionFragmentJsonFormat: OFormat[CreateChimericTransactionFragment] =
    new OFormat[CreateChimericTransactionFragment] {
      def read(json: JsValue): JsResult[CreateChimericTransactionFragment] =
        for {
          fragment <- ChimericTxFragmentJsonFormat.reads(json)
          keyOpt <- CouldHaveSigningPrivateKeyJsonFormat.reads(json)
          result <- fragment match {
            case s: SignableChimericTxFragment if keyOpt.signingPrivateKey.isDefined =>
              CreateSignableChimericTransactionFragment(s, keyOpt.signingPrivateKey.get)
            case _: SignableChimericTxFragment if keyOpt.signingPrivateKey.isEmpty =>
              JsError(s"Missing signingPrivateKey")
            case n: NonSignableChimericTxFragment =>
              CreateNonSignableChimericTransactionFragment(n)
          }
        } yield result

      def write(obj: CreateChimericTransactionFragment): JsObject = {
        val base = ChimericTxFragmentJsonFormat.writes(obj.fragment)
        obj match {
          case _: CreateNonSignableChimericTransactionFragment =>
            base
          case s: CreateSignableChimericTransactionFragment =>
            val signature = HasSigningPrivateKeyJsonFormat.writes(HasSigningPrivateKey(s.signingPrivateKey))
            base ++ signature
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
    signingPrivateKey: SigningPrivateKey
) extends CreateChimericTransactionFragment {
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

  implicit val createChimericTransactionRequestJsonFormat: OFormat[CreateChimericTransactionRequest] = Json.format

}

case class SubmitChimericTransactionFragment(fragment: ChimericTxFragment) {
  def `type`: ChimericTransactionFragmentType =
    ChimericTransactionFragmentType.of(fragment)
}
case class SubmitChimericTransactionRequest(fragments: Seq[SubmitChimericTransactionFragment], ledgerId: LedgerId)

object ChimericTransactionRequest extends ExtraJsonFormats {
  implicit val submitChimericTransactionFragmentJsonFormat: OFormat[SubmitChimericTransactionFragment] =
    new OFormat[SubmitChimericTransactionFragment] {
      def reads(json: JsValue): JsResult[SubmitChimericTransactionFragment] =
        SubmitChimericTransactionFragment(json.convertTo[ChimericTxFragment])

      def writes(obj: SubmitChimericTransactionFragment): JsValue =
        obj.fragment.toJson
    }

  implicit val submitChimericTransactionRequestJsonFormat: OFormat[SubmitChimericTransactionRequest] = Json.format
}
