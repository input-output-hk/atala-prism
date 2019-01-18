package io.iohk.cef.frontend.models

import io.iohk.cef.ledger.chimeric._
import play.api.libs.json._

trait ChimericTxFragmentFormat {

  /**
    * {{{
    *
    *   # Some setup
    *   >>> import spray.json._
    *   >>> import io.iohk.cef.ledger.chimeric._
    *   >>> val formats = new ChimericTxFragmentFormat{}
    *   >>> import formats._
    *
    *   # Convert a value into json
    *   >>> Value(Map("USD" -> BigDecimal(123))).toJson
    *   {"USD":123}
    *
    *   # Recover a value from a json
    *   >>> """{"USD":123}""".parseJson.convertTo[Value]
    *   Value(Map(USD -> 123))
    * }}}
    */
  implicit val valueJsonFormat = Json.format[Value]

  /**
    * {{{
    *
    *   # Some setup
    *   >>> import spray.json._
    *   >>> import io.iohk.cef.ledger.chimeric._
    *   >>> val formats = new ChimericTxFragmentFormat{}
    *   >>> import formats._
    *
    *   # Convert a value into json
    *   >>> TxOutRef("abc", 456).toJson
    *   {"txId":"abc","index":456}
    *
    *   # Recover a value from a json
    *   >>> """{"txId":"abc","index":456}""".parseJson.convertTo[TxOutRef]
    *   TxOutRef(abc,456)
    * }}}
    */
  implicit val TxOutRefJsonFormat: OFormat[TxOutRef] = Json.format

  implicit val SignatureTxFragmentJsonFormat: OFormat[SignatureTxFragment] = Json.format

  implicit val WithdrawalJsonFormat: OFormat[Withdrawal] = Json.format

  implicit val MintJsonFormat: OFormat[Mint] = Json.format

  implicit val InputJsonFormat: OFormat[Input] = Json.format

  implicit val FeeJsonFormat: OFormat[Fee] = Json.format

  implicit val OutputJsonFormat: OFormat[Output] = Json.format

  implicit val DepositJsonFormat: OFormat[Deposit] = Json.format

  implicit val CreateCurrencyJsonFormat: OFormat[CreateCurrency] = Json.format

  protected case class HasTxFragmentType(`type`: ChimericTransactionFragmentType)
  protected implicit val HasTxFragmentTypeJsonFormat: OFormat[HasTxFragmentType] = Json.format

  /**
    * {{{
    *
    *   # Some setup
    *   >>> import spray.json._
    *   >>> import io.iohk.cef.ledger.chimeric._
    *   >>> val formats = new ChimericTxFragmentFormat{}
    *   >>> import formats._
    *
    *   # Convert a value into json
    *   >>> val tx: ChimericTxFragment = Mint(Value(Map("USD" -> BigDecimal(123))))
    *   >>> tx.toJson
    *   {"value":{"USD":123},"type":"Mint"}
    *
    *   # Recover a value from a json
    *   >>> """{"value":{"USD":123},"type":"Mint"}""".parseJson.convertTo[ChimericTxFragment]
    *   Mint(Value(Map(USD -> 123)))
    * }}}
    */
  implicit val ChimericTxFragmentJsonFormat: OFormat[ChimericTxFragment] =
    new OFormat[ChimericTxFragment] {
      override def reads(json: JsValue): JsResult[ChimericTxFragment] =
        for {
          hasValue <- HasTxFragmentTypeJsonFormat.reads(json)
          result <- hasValue.`type` match {
            case ChimericTransactionFragmentType.Withdrawal => WithdrawalJsonFormat.reads(json)
            case ChimericTransactionFragmentType.Input => InputJsonFormat.reads(json)
            case ChimericTransactionFragmentType.Mint => MintJsonFormat.reads(json)
            case ChimericTransactionFragmentType.Fee => FeeJsonFormat.reads(json)
            case ChimericTransactionFragmentType.Output => OutputJsonFormat.reads(json)
            case ChimericTransactionFragmentType.Deposit => DepositJsonFormat.reads(json)
            case ChimericTransactionFragmentType.CreateCurrency => CreateCurrencyJsonFormat.reads(json)
            case ChimericTransactionFragmentType.SignatureTxFragment => SignatureTxFragmentJsonFormat.reads(json)
          }
        } yield result

      override def writes(obj: ChimericTxFragment): JsObject = {
        val `type` = ChimericTransactionFragmentType.of(obj)
        val json = obj match {
          case f: Withdrawal => WithdrawalJsonFormat.writes(f)
          case f: Mint => MintJsonFormat.writes(f)
          case f: Input => InputJsonFormat.writes(f)
          case f: Fee => FeeJsonFormat.writes(f)
          case f: Output => OutputJsonFormat.writes(f)
          case f: Deposit => DepositJsonFormat.writes(f)
          case f: CreateCurrency => CreateCurrencyJsonFormat.writes(f)
          case f: SignatureTxFragment => SignatureTxFragmentJsonFormat.writes(f)
        }

        val part = HasTxFragmentType(`type`)

        val baseObj = HasTxFragmentTypeJsonFormat.writes(part)

        baseObj ++ json
      }
    }

}
