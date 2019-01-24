package io.iohk.cef.frontend.models

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.chimeric._
import play.api.libs.json._

trait ChimericCodecs {

  implicit val valueFormat: Format[Value] = new Format[Value] {
    override def writes(o: Value): JsValue = {
      Json.toJson(o.m)
    }

    override def reads(json: JsValue): JsResult[Value] = {
      json
        .validate[Map[Currency, Quantity]]
        .map(Value.apply)
    }
  }

  implicit val withdrawalFormat: Format[Withdrawal] = Json.format[Withdrawal]

  implicit val txOutRefFormat: Format[TxOutRef] = Json.format[TxOutRef]

  implicit val depositFormat: Format[Deposit] = Json.format[Deposit]

  implicit val mintFormat: Format[Mint] = new Format[Mint] {
    override def writes(o: Mint): JsValue = {
      Json.toJson(o.value)(valueFormat)
    }

    override def reads(json: JsValue): JsResult[Mint] = {
      json
        .validate[Value](valueFormat)
        .map(Mint.apply)
    }
  }

  implicit val feeFormat: Format[Fee] = new Format[Fee] {
    override def writes(o: Fee): JsValue = {
      Json.toJson(o.value)(valueFormat)
    }

    override def reads(json: JsValue): JsResult[Fee] = {
      json
        .validate[Value](valueFormat)
        .map(Fee.apply)
    }
  }

  implicit val createCurrencyFormat: Format[CreateCurrency] = new Format[CreateCurrency] {
    override def writes(o: CreateCurrency): JsValue = {
      JsString(o.currency)
    }

    override def reads(json: JsValue): JsResult[CreateCurrency] = {
      json.validate[String].map(CreateCurrency.apply)
    }
  }

  implicit val signatureTxFragmentFormat: Format[SignatureTxFragment] = new Format[SignatureTxFragment] {
    override def writes(o: SignatureTxFragment): JsValue = {
      Json.toJson(o.signature)
    }

    override def reads(json: JsValue): JsResult[SignatureTxFragment] = {
      json.validate[Signature].map(SignatureTxFragment.apply)
    }
  }

  implicit val inputFormat: Format[Input] = new Format[Input] {
    override def writes(o: Input): JsValue = {
      Json.obj("txOutRef" -> Json.toJson(o.txOutRef)(txOutRefFormat), "value" -> Json.toJson(o.value)(valueFormat))
    }

    override def reads(json: JsValue): JsResult[Input] = {
      for {
        txOutRef <- (json \ "txOutRef").validate[TxOutRef](txOutRefFormat)
        value <- (json \ "value").validate[Value](valueFormat)
      } yield Input(txOutRef, value)
    }
  }

  implicit val outputFormat: Format[Output] = new Format[Output] {
    override def writes(o: Output): JsValue = {
      Json.obj("value" -> Json.toJson(o.value)(valueFormat), "signingPublicKey" -> Json.toJson(o.signingPublicKey))
    }

    override def reads(json: JsValue): JsResult[Output] = {
      for {
        value <- (json \ "value").validate[Value](valueFormat)
        signingPublicKey <- (json \ "signingPublicKey").validate[SigningPublicKey]
      } yield Output(value, signingPublicKey)
    }
  }

  implicit val chimericTxFragmentFormat: Format[ChimericTxFragment] = new Format[ChimericTxFragment] {
    override def writes(o: ChimericTxFragment): JsValue = {
      val (tpe, obj) = o match {
        case obj: Withdrawal => "withdrawal" -> Json.toJson(obj)(withdrawalFormat)
        case obj: Mint => "mint" -> Json.toJson(obj)(mintFormat)
        case obj: Fee => "fee" -> Json.toJson(obj)(feeFormat)
        case obj: Deposit => "deposit" -> Json.toJson(obj)(depositFormat)
        case obj: CreateCurrency => "createCurrency" -> Json.toJson(obj)(createCurrencyFormat)
        case obj: SignatureTxFragment => "signature" -> Json.toJson(obj)(signatureTxFragmentFormat)
        case obj: Input => "input" -> Json.toJson(obj)(inputFormat)
        case obj: Output => "output" -> Json.toJson(obj)(outputFormat)
      }

      Json.obj("type" -> tpe, "obj" -> obj)
    }

    override def reads(json: JsValue): JsResult[ChimericTxFragment] = {
      val obj = json \ "obj"

      (json \ "type").validate[String] match {
        case JsSuccess("withdrawal", _) => obj.validate[Withdrawal](withdrawalFormat)
        case JsSuccess("mint", _) => obj.validate[Mint](mintFormat)
        case JsSuccess("fee", _) => obj.validate[Fee](feeFormat)
        case JsSuccess("deposit", _) => obj.validate[Deposit](depositFormat)
        case JsSuccess("createCurrency", _) => obj.validate[CreateCurrency](createCurrencyFormat)
        case JsSuccess("signature", _) => obj.validate[SignatureTxFragment](signatureTxFragmentFormat)
        case JsSuccess("input", _) => obj.validate[Input](inputFormat)
        case JsSuccess("output", _) => obj.validate[Output](outputFormat)
        case JsSuccess(_, _) => JsError("Invalid ChimericTxFragment")
        case x: JsError => x
      }
    }
  }

  implicit val chimericTxFormat: Format[ChimericTx] = Json.format[ChimericTx]

  implicit lazy val createNonSignableChimericTransactionFragmentFormat
      : Format[CreateNonSignableChimericTransactionFragment] =
    new Format[CreateNonSignableChimericTransactionFragment] {

      override def writes(o: CreateNonSignableChimericTransactionFragment): JsValue = {
        Json.obj("fragment" -> Json.toJson(o.fragment))
      }

      override def reads(json: JsValue): JsResult[CreateNonSignableChimericTransactionFragment] = {
        for {
          fragment <- (json \ "fragment")
            .validate[ChimericTxFragment]
            .collect(JsonValidationError("NonSignableChimericTxFragment expected")) {
              case x: NonSignableChimericTxFragment => x
            }
        } yield CreateNonSignableChimericTransactionFragment(fragment)
      }
    }

  implicit lazy val createSignableChimericTransactionFragmentFormat: Format[CreateSignableChimericTransactionFragment] =
    new Format[CreateSignableChimericTransactionFragment] {
      override def writes(o: CreateSignableChimericTransactionFragment): JsValue = {
        Json.obj(
          "fragment" -> Json.toJson(o.fragment),
          "signingPrivateKey" -> Json.toJson(o.signingPrivateKey)
        )
      }

      override def reads(json: JsValue): JsResult[CreateSignableChimericTransactionFragment] = {
        for {
          fragment <- (json \ "fragment")
            .validate[ChimericTxFragment]
            .collect(JsonValidationError("SignableChimericTxFragment expected")) {
              case x: SignableChimericTxFragment => x
            }

          signingPrivateKey <- (json \ "signingPrivateKey").validate[SigningPrivateKey]
        } yield CreateSignableChimericTransactionFragment(fragment, signingPrivateKey)
      }
    }

  implicit lazy val createChimericTransactionFragmentFormat: Format[CreateChimericTransactionFragment] =
    new Format[CreateChimericTransactionFragment] {
      override def writes(o: CreateChimericTransactionFragment): JsValue = {
        val (tpe, json) = o match {
          case x: CreateNonSignableChimericTransactionFragment =>
            "nonSignableFragment" -> Json.toJson(x)(createNonSignableChimericTransactionFragmentFormat)

          case x: CreateSignableChimericTransactionFragment =>
            "signableFragment" -> Json.toJson(x)(createSignableChimericTransactionFragmentFormat)
        }

        val map = Map(
          "type" -> JsString(tpe),
          "obj" -> json
        )

        JsObject(map)
      }

      override def reads(json: JsValue): JsResult[CreateChimericTransactionFragment] = {
        val obj = json \ "obj"
        (json \ "type")
          .validate[String]
          .flatMap {
            case "nonSignableFragment" => obj.validate[CreateNonSignableChimericTransactionFragment]
            case "signableFragment" => obj.validate[CreateSignableChimericTransactionFragment]
            case _ => JsError("Unknown type")
          }
      }
    }

  implicit val createChimericTransactionRequestFormat: Format[CreateChimericTransactionRequest] =
    Json.format[CreateChimericTransactionRequest]
}
