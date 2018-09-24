package io.iohk.cef.frontend.models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.chimeric._
import spray.json._

import scala.collection.immutable.Map

case class ChimericTransactionRequest(transaction: ChimericTx, ledgerId: LedgerId)

object ChimericTransactionRequest extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object ValueJsonFormat extends JsonFormat[Value] {
    override def read(json: JsValue): Value = {
      val map = json.convertTo[Map[String, BigDecimal]]
      Value(map)
    }

    override def write(obj: Value): JsValue = {
      obj.iterator.toMap.toJson
    }
  }

  implicit object WithdrawalJsonFormat extends JsonFormat[Withdrawal] {

    override def read(json: JsValue): Withdrawal = {
      val fields = json.asJsObject().fields

      (fields("address"), fields("value"), fields("nonce")) match {
        case (JsString(address), valueJson, JsNumber(nonce)) =>
          val value = valueJson.convertTo[Value]
          Withdrawal(address, value, nonce.toIntExact)

        case _ => deserializationError("Invalid withdrawal")
      }
    }

    override def write(obj: Withdrawal): JsValue = {
      JsObject(
        "address" -> JsString(obj.address),
        "value" -> obj.value.toJson,
        "nonce" -> JsNumber(obj.nonce)
      )
    }
  }

  implicit object MintJsonFormat extends JsonFormat[Mint] {
    override def read(json: JsValue): Mint = {
      val fields = json.asJsObject().fields

      fields("value") match {
        case obj: JsObject =>
          val value = obj.convertTo[Value]
          Mint(value)

        case _ => deserializationError("Invalid mint")
      }
    }

    override def write(obj: Mint): JsValue = {
      JsObject("value" -> obj.value.toJson)
    }
  }

  implicit object TxOutRefJsonFormat extends JsonFormat[TxOutRef] {
    override def read(json: JsValue): TxOutRef = {
      val fields = json.asJsObject().fields

      (fields("index"), fields("txid")) match {
        case (JsNumber(index), JsString(txid)) =>
          TxOutRef(txid, index.toIntExact)

        case _ => deserializationError("Invalid TxOutRef")
      }
    }

    override def write(obj: TxOutRef): JsValue = {
      JsObject(
        "index" -> JsNumber(obj.index),
        "txid" -> JsString(obj.txId)
      )
    }
  }

  implicit object InputJsonFormat extends JsonFormat[Input] {
    override def read(json: JsValue): Input = {
      val fields = json.asJsObject().fields

      (fields("txOutRef"), fields("value")) match {
        case (txOutRef: JsObject, value: JsObject) =>
          Input(txOutRef.convertTo[TxOutRef], value.convertTo[Value])

        case _ => deserializationError("Invalid input")
      }
    }

    override def write(obj: Input): JsValue = {
      JsObject(
        "txOutRef" -> obj.txOutRef.toJson,
        "value" -> obj.value.toJson
      )
    }
  }

  implicit object FeeJsonFormat extends JsonFormat[Fee] {
    override def read(json: JsValue): Fee = {
      val fields = json.asJsObject().fields

      fields("value") match {
        case obj: JsObject =>
          val value = obj.convertTo[Value]
          Fee(value)

        case _ => deserializationError("Invalid fee")
      }
    }

    override def write(obj: Fee): JsValue = {
      JsObject("value" -> obj.value.toJson)
    }
  }

  implicit object OutputJsonFormat extends JsonFormat[Output] {
    override def read(json: JsValue): Output = {
      val fields = json.asJsObject().fields

      fields("value") match {
        case obj: JsObject =>
          val value = obj.convertTo[Value]
          Output(value)

        case _ => deserializationError("Invalid output")
      }
    }

    override def write(obj: Output): JsValue = {
      JsObject("value" -> obj.value.toJson)
    }
  }

  implicit object DepositJsonFormat extends JsonFormat[Deposit] {

    override def read(json: JsValue): Deposit = {
      val fields = json.asJsObject().fields

      (fields("address"), fields("value")) match {
        case (JsString(address), valueJson) =>
          val value = valueJson.convertTo[Value]
          Deposit(address, value)

        case _ => deserializationError("Invalid deposit")
      }
    }

    override def write(obj: Deposit): JsValue = {
      JsObject(
        "address" -> JsString(obj.address),
        "value" -> obj.value.toJson
      )
    }
  }

  implicit object CreateCurrencyJsonFormat extends JsonFormat[CreateCurrency] {

    override def read(json: JsValue): CreateCurrency = {
      val fields = json.asJsObject().fields

      fields("currency") match {
        case JsString(currency) =>
          CreateCurrency(currency)

        case _ => deserializationError("Invalid create currency")
      }
    }

    override def write(obj: CreateCurrency): JsValue = {
      JsObject(
        "currency" -> JsString(obj.currency)
      )
    }
  }

  implicit object ChimericTxFragmentJsonFormat extends JsonFormat[ChimericTxFragment] {

    private val WithdrawalType = "Withdrawal"
    private val MintType = "Mint"
    private val InputType = "Input"
    private val FeeType = "Fee"
    private val OutputType = "Output"
    private val DepositType = "Deposit"
    private val CreateCurrencyType = "CreateCurrency"

    override def read(json: JsValue): ChimericTxFragment = {

      val fields = json.asJsObject().fields

      (fields.get("type"), fields.get("fragment")) match {
        case (Some(JsString(WithdrawalType)), Some(fragment: JsObject)) => fragment.convertTo[Withdrawal]
        case (Some(JsString(MintType)), Some(fragment: JsObject)) => fragment.convertTo[Mint]
        case (Some(JsString(InputType)), Some(fragment: JsObject)) => fragment.convertTo[Input]
        case (Some(JsString(FeeType)), Some(fragment: JsObject)) => fragment.convertTo[Fee]
        case (Some(JsString(OutputType)), Some(fragment: JsObject)) => fragment.convertTo[Output]
        case (Some(JsString(DepositType)), Some(fragment: JsObject)) => fragment.convertTo[Deposit]
        case (Some(JsString(CreateCurrencyType)), Some(fragment: JsObject)) => fragment.convertTo[CreateCurrency]
        case _ => deserializationError("Invalid chimeric transaction request")
      }
    }

    override def write(obj: ChimericTxFragment): JsValue = {
      val (tpe, json) = obj match {
        case fragment: Withdrawal => (WithdrawalType, fragment.toJson)
        case fragment: Mint => (MintType, fragment.toJson)
        case fragment: Input => (InputType, fragment.toJson)
        case fragment: Fee => (FeeType, fragment.toJson)
        case fragment: Output => (OutputType, fragment.toJson)
        case fragment: Deposit => (DepositType, fragment.toJson)
        case fragment: CreateCurrency => (CreateCurrencyType, fragment.toJson)
      }

      JsObject(
        "type" -> JsString(tpe),
        "fragment" -> json
      )
    }
  }

  implicit object ChimericTxJsonFormat extends JsonFormat[ChimericTx] {
    override def read(json: JsValue): ChimericTx = {
      val fields = json.asJsObject().fields

      val fragments = fields("fragments") match {
        case array: JsArray => array.convertTo[List[ChimericTxFragment]]
        case _ => deserializationError("Invalid chimeric tx")
      }

      ChimericTx(fragments)
    }

    override def write(obj: ChimericTx): JsValue = {
      JsObject("fragments" -> obj.fragments.toJson)
    }
  }

  implicit val chimericTransactionRequestJsonFormat: RootJsonFormat[ChimericTransactionRequest] = jsonFormat2(
    ChimericTransactionRequest.apply)
}
