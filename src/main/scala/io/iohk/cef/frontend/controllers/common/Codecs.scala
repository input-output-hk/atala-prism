package io.iohk.cef.frontend.controllers.common

import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.data.{DataItem, Owner, Witness}
import io.iohk.cef.frontend.models.{
  ChimericTransactionFragmentType,
  CreateChimericTransactionFragment,
  CreateChimericTransactionRequest,
  CreateIdentityTransactionRequest,
  CreateNonSignableChimericTransactionFragment,
  CreateSignableChimericTransactionFragment,
  IdentityTransactionType
}
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.identity.{Claim, IdentityTransaction, Link, Unlink}
import io.iohk.cef.utils.ByteStringExtension
import play.api.libs.json._

import scala.util.Try

object Codecs {

  private def parseCryptoObject[A, B](json: JsValue, fieldName: String, f: ByteString => Either[B, A]) = {
    json
      .asOpt[String]
      .map { string =>
        Try(fromHex(string)).toOption
          .map(f)
          .flatMap(_.toOption)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Invalid $fieldName"))
      }
      .getOrElse(JsError(s"Missing $fieldName"))
  }

  implicit lazy val signingPublicKeyFormat: Format[SigningPublicKey] = new Format[SigningPublicKey] {
    override def writes(o: SigningPublicKey): JsValue = {
      JsString(toCleanHex(o.toByteString))
    }

    override def reads(json: JsValue): JsResult[SigningPublicKey] = {
      parseCryptoObject(json, "public key", SigningPublicKey.decodeFrom)
    }
  }

  implicit lazy val signingPrivateKeyFormat: Format[SigningPrivateKey] = new Format[SigningPrivateKey] {
    override def writes(o: SigningPrivateKey): JsValue = {
      JsString(toCleanHex(o.toByteString))
    }

    override def reads(json: JsValue): JsResult[SigningPrivateKey] = {
      parseCryptoObject(json, "private key", SigningPrivateKey.decodeFrom)
    }
  }

  implicit lazy val signatureFormat: Format[Signature] = new Format[Signature] {
    override def writes(o: Signature): JsValue = {
      JsString(toCleanHex(o.toByteString))
    }

    override def reads(json: JsValue): JsResult[Signature] = {
      parseCryptoObject(json, "signature", Signature.decodeFrom)
    }
  }

  implicit val signingKeyPairWrites: Writes[SigningKeyPair] = Writes { obj =>
    val map = Map(
      "publicKey" -> JsString(toCleanHex(obj.public.toByteString)),
      "privateKey" -> JsString(toCleanHex(obj.`private`.toByteString))
    )

    JsObject(map)
  }

  implicit lazy val valueFormat: Format[Value] = Json.format[Value]
  implicit lazy val withdrawalFormat: Format[Withdrawal] = Json.format[Withdrawal]
  implicit lazy val mintFormat: Format[Mint] = Json.format[Mint]
  implicit lazy val txOutRefFormat: Format[TxOutRef] = Json.format[TxOutRef]
  implicit lazy val inputFormat: Format[Input] = Json.format[Input]
  implicit lazy val feeFormat: Format[Fee] = Json.format[Fee]
  implicit lazy val outputFormat: Format[Output] = Json.format[Output]
  implicit lazy val createCurrencyFormat: Format[CreateCurrency] = Json.format[CreateCurrency]
  implicit lazy val depositFormat: Format[Deposit] = Json.format[Deposit]
  implicit lazy val signatureTxFragmentFormat: Format[SignatureTxFragment] = Json.format[SignatureTxFragment]

  implicit lazy val chimericTxFragmentFormat: Format[ChimericTxFragment] = new Format[ChimericTxFragment] {
    override def writes(o: ChimericTxFragment): JsValue = {
      val (tpe, json) = o match {
        case x: Withdrawal => ("withdrawal", Json.toJson(x)(withdrawalFormat))
        case x: Mint => ("mint", Json.toJson(x)(mintFormat))
        case x: Input => ("input", Json.toJson(x)(inputFormat))
        case x: Fee => ("fee", Json.toJson(x)(feeFormat))
        case x: Output => ("output", Json.toJson(x)(outputFormat))
        case x: CreateCurrency => ("createCurrency", Json.toJson(x)(createCurrencyFormat))
        case x: Deposit => ("deposit", Json.toJson(x)(depositFormat))
        case x: SignatureTxFragment => ("signatureTxFragment", Json.toJson(x)(signatureTxFragmentFormat))
      }

      val map = Map("type" -> JsString(tpe), "fragment" -> json)

      JsObject(map)
    }

    override def reads(json: JsValue): JsResult[ChimericTxFragment] = {
      val fragment = json \ "fragment"

      (json \ "type").asOpt[String] match {
        case Some("withdrawal") => fragment.validate[Withdrawal]
        case Some("mint") => fragment.validate[Mint]
        case Some("fee") => fragment.validate[Fee]
        case Some("output") => fragment.validate[Output]
        case Some("createCurrency") => fragment.validate[CreateCurrency]
        case Some("deposit") => fragment.validate[Deposit]
        case Some("signatureTxFragment") => fragment.validate[SignatureTxFragment]
        case _ => JsError("Invalid ChimericTxFragment")
      }
    }
  }

  implicit val nonSignableChimericTxFragmentFormat: Format[NonSignableChimericTxFragment] =
    new Format[NonSignableChimericTxFragment] {
      override def writes(o: NonSignableChimericTxFragment): JsValue = {
        Json.toJson(o)(chimericTxFragmentFormat)
      }

      override def reads(json: JsValue): JsResult[NonSignableChimericTxFragment] = {
        val fragment = json \ "fragment"

        (json \ "type").asOpt[String] match {
          case Some("mint") => fragment.validate[Mint]
          case Some("fee") => fragment.validate[Fee]
          case Some("output") => fragment.validate[Output]
          case Some("createCurrency") => fragment.validate[CreateCurrency]
          case Some("deposit") => fragment.validate[Deposit]
          case Some("signatureTxFragment") => fragment.validate[SignatureTxFragment]
          case _ => JsError("Missing or invalid NonSignableChimericTxFragment")
        }
      }
    }

  implicit val signableChimericTxFragmentFormat: Format[SignableChimericTxFragment] =
    new Format[SignableChimericTxFragment] {
      override def writes(o: SignableChimericTxFragment): JsValue = {
        Json.toJson(o)(chimericTxFragmentFormat)
      }

      override def reads(json: JsValue): JsResult[SignableChimericTxFragment] = {
        val fragment = json \ "fragment"

        (json \ "type").asOpt[String] match {
          case Some("input") => fragment.validate[Input]
          case Some("withdrawal") => fragment.validate[Withdrawal]
          case _ => JsError("Missing or invalid SignableChimericTxFragment")
        }
      }
    }

  implicit lazy val chimericTxFormat: Format[ChimericTx] = Json.format[ChimericTx]
  implicit lazy val createChimericTransactionRequestFormat: Format[CreateChimericTransactionRequest] =
    Json.format[CreateChimericTransactionRequest]

  implicit lazy val chimericTransactionFragmentType: Format[ChimericTransactionFragmentType] =
    new Format[ChimericTransactionFragmentType] {
      override def writes(o: ChimericTransactionFragmentType): JsValue = {
        JsString(o.toString)
      }

      override def reads(json: JsValue): JsResult[ChimericTransactionFragmentType] = {
        val maybe = json
          .asOpt[String]
          .flatMap { string =>
            ChimericTransactionFragmentType.withNameInsensitiveOption(string)
          }

        maybe
          .map(JsSuccess(_))
          .getOrElse(JsError("Invalid or missing chimeric transaction fragment type"))
      }
    }

  implicit lazy val createNonSignableChimericTransactionFragmentFormat
    : Format[CreateNonSignableChimericTransactionFragment] = Json.format[CreateNonSignableChimericTransactionFragment]

  implicit lazy val createSignableChimericTransactionFragmentFormat: Format[CreateSignableChimericTransactionFragment] =
    Json.format[CreateSignableChimericTransactionFragment]

  implicit lazy val createChimericTransactionFragmentFormat: Format[CreateChimericTransactionFragment] =
    new Format[CreateChimericTransactionFragment] {
      override def writes(o: CreateChimericTransactionFragment): JsValue = {
        val (tpe, json) = o match {
          case x: CreateNonSignableChimericTransactionFragment =>
            (
              "createNonSignableChimericTransactionFragment",
              Json.toJson(x)(createNonSignableChimericTransactionFragmentFormat))
          case x: CreateSignableChimericTransactionFragment =>
            (
              "createSignableChimericTransactionFragment",
              Json.toJson(x)(createSignableChimericTransactionFragmentFormat))
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
          .asOpt[String]
          .flatMap {
            case "createNonSignableChimericTransactionFragment" =>
              obj.asOpt[CreateNonSignableChimericTransactionFragment]
            case "createSignableChimericTransactionFragment" => obj.asOpt[CreateSignableChimericTransactionFragment]
            case _ => None
          }
          .map(JsSuccess(_))
          .getOrElse(JsError("Missing or invalid create chimeric transaction fragment"))
      }
    }

  implicit val transactionTypeReads: Reads[IdentityTransactionType] = Reads { json =>
    json.validate[String].flatMap { string =>
      IdentityTransactionType
        .withNameInsensitiveOption(string)
        .map(JsSuccess.apply(_))
        .getOrElse {
          JsError.apply("Invalid transaction type")
        }
    }
  }

  implicit val witnessFormat: Format[Witness] = Json.format[Witness]

  implicit val ownerFormat: Format[Owner] = Json.format[Owner]

  implicit def dataItemFormat[T](implicit tFormat: Format[T]): Format[DataItem[T]] = Json.format[DataItem[T]]

  implicit val requestReads: Reads[CreateIdentityTransactionRequest] = Json.reads[CreateIdentityTransactionRequest]

  implicit val responseWrites: Writes[IdentityTransaction] = Writes[IdentityTransaction] { obj =>
    val tpe = obj match {
      case _: Claim => IdentityTransactionType.Claim
      case _: Link => IdentityTransactionType.Link
      case _: Unlink => IdentityTransactionType.Unlink
    }

    val map = Map(
      "type" -> JsString(tpe.toString),
      "key" -> JsString(toCleanHex(obj.key.toByteString)),
      "signature" -> JsString(toCleanHex(obj.signature.toByteString)),
      "identity" -> JsString(obj.identity)
    )

    JsObject(map)
  }

  // TODO: Move to the util package and test it
  def fromHex(hex: String): ByteString = {
    val array = hex
      .sliding(2, 2)
      .toArray
      .map(s => Integer.parseInt(s, 16).toByte)

    ByteString(array)
  }

  // TODO: Move to the util package and test it
  def toCleanHex(byteString: ByteString): String = {
    byteString.toHex
      .replace("\n", "")
      .replace(" ", "")
  }
}
