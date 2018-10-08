package io.iohk.cef.frontend.models

import spray.json._
import io.iohk.cef.ledger.chimeric._

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
  implicit object ValueJsonFormat extends JsonFormat[Value] {
    override def read(json: JsValue): Value = {
      val map = json.convertTo[Map[String, BigDecimal]]
      Value(map)
    }

    override def write(obj: Value): JsValue = {
      obj.iterator.toMap.toJson
    }
  }

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
  implicit val TxOutRefJsonFormat: RootJsonFormat[TxOutRef] =
    jsonFormat2(TxOutRef)

  implicit val signatureTxFragmentJsonFormat: JsonFormat[SignatureTxFragment] =
    jsonFormat1(SignatureTxFragment.apply)

  implicit val WithdrawalJsonFormat: RootJsonFormat[Withdrawal] =
    jsonFormat3(Withdrawal)

  implicit val MintJsonFormat: RootJsonFormat[Mint] =
    jsonFormat1(Mint)

  implicit val InputJsonFormat: RootJsonFormat[Input] =
    jsonFormat2(Input)

  implicit val FeeJsonFormat: RootJsonFormat[Fee] =
    jsonFormat1(Fee)

  implicit val OutputJsonFormat: RootJsonFormat[Output] =
    jsonFormat2(Output)

  implicit val DepositJsonFormat: RootJsonFormat[Deposit] =
    jsonFormat3(Deposit)

  implicit val CreateCurrencyJsonFormat: RootJsonFormat[CreateCurrency] =
    jsonFormat1(CreateCurrency)

  protected case class HasTxFragmentType(`type`: ChimericTransactionFragmentType)
  protected implicit val HasTxFragmentTypeJsonFormat: RootJsonFormat[HasTxFragmentType] =
    jsonFormat1(HasTxFragmentType.apply _)

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
  implicit val ChimericTxFragmentJsonFormat: JsonFormat[ChimericTxFragment] =
    new JsonFormat[ChimericTxFragment] {
      override def read(json: JsValue): ChimericTxFragment = {
        json.convertTo[HasTxFragmentType].`type` match {
          case ChimericTransactionFragmentType.Withdrawal => json.convertTo[Withdrawal]
          case ChimericTransactionFragmentType.Input => json.convertTo[Input]
          case ChimericTransactionFragmentType.Mint => json.convertTo[Mint]
          case ChimericTransactionFragmentType.Fee => json.convertTo[Fee]
          case ChimericTransactionFragmentType.Output => json.convertTo[Output]
          case ChimericTransactionFragmentType.Deposit => json.convertTo[Deposit]
          case ChimericTransactionFragmentType.CreateCurrency => json.convertTo[CreateCurrency]
          case ChimericTransactionFragmentType.SignatureTxFragment => json.convertTo[SignatureTxFragment]
        }
      }

      override def write(obj: ChimericTxFragment): JsValue = {
        val `type` = ChimericTransactionFragmentType.of(obj)
        val json = obj match {
          case f: Withdrawal => f.toJson
          case f: Mint => f.toJson
          case f: Input => f.toJson
          case f: Fee => f.toJson
          case f: Output => f.toJson
          case f: Deposit => f.toJson
          case f: CreateCurrency => f.toJson
          case f: SignatureTxFragment => f.toJson
        }

        val part = HasTxFragmentType(`type`)

        JsObject(json.asJsObject.fields ++ part.toJson.asJsObject.fields)
      }
    }

}
