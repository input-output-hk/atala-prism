package io.iohk.cef.frontend.controllers.common

import akka.util.ByteString
import io.iohk.cef.transactionservice._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.Query._
import io.iohk.cef.data.query.{Value => ValueRef}
import io.iohk.cef.data.query.Value.{
  BooleanRef,
  ByteRef,
  CharRef,
  DoubleRef,
  FloatRef,
  IntRef,
  LongRef,
  ShortRef,
  StringRef
}
import io.iohk.cef.data.query.{Field, Query}
import io.iohk.cef.frontend.models._
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.identity._
import io.iohk.cef.network.NodeId
import io.iohk.cef.transactionservice._
import io.iohk.cef.utils.ByteStringExtension
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

object Codecs {

  implicit def seqFormat[T](implicit tFormat: Format[T]): Format[Seq[T]] = new Format[Seq[T]] {
    override def reads(json: JsValue): JsResult[Seq[T]] = {
      val seqResult = json match {
        case JsArray(values) =>
          values.map(_.validate[T](tFormat)).foldLeft[JsResult[List[T]]](JsSuccess(List())) { (state, current) =>
            for {
              s <- state
              c <- current
            } yield c :: s
          }
        case _ => JsError("Invalid sequence detected")
      }
      seqResult.map(_.reverse)
    }

    override def writes(o: Seq[T]): JsValue = {
      val jsonSeq = o.map(Json.toJson(_))
      JsArray(jsonSeq)
    }
  }

  implicit val fieldQueryFormat = Json.format[Field]
  implicit val doubleRefQueryFormat = Json.format[DoubleRef]
  implicit val floatRefQueryFormat = Json.format[FloatRef]
  implicit val longRefQueryFormat = Json.format[LongRef]
  implicit val intRefQueryFormat = Json.format[IntRef]
  implicit val shortRefQueryFormat = Json.format[ShortRef]
  implicit val byteRefQueryFormat = Json.format[ByteRef]
  implicit val booleanRefQueryFormat = Json.format[BooleanRef]
  implicit val stringRefQueryFormat = Json.format[StringRef]
  implicit val charRefQueryFormat = new Format[CharRef] {
    override def reads(json: JsValue): JsResult[CharRef] =
      (json \ "value").validate[String].filter(JsError("Invalid length char"))(_.size == 1).map(_.toCharArray.head)

    override def writes(o: CharRef): JsValue = {
      JsObject(Map("value" -> JsString(o.value.toString)))
    }
  }

  implicit val valueQueryFormat = new Format[ValueRef] {
    override def reads(json: JsValue): JsResult[ValueRef] = {
      (json \ "type").validate[String] match {
        case JsSuccess("doubleRef", _) => json.validate[DoubleRef]
        case JsSuccess("floatRef", _) => json.validate[FloatRef]
        case JsSuccess("longRef", _) => json.validate[LongRef]
        case JsSuccess("intRef", _) => json.validate[IntRef]
        case JsSuccess("shortRef", _) => json.validate[ShortRef]
        case JsSuccess("byteRef", _) => json.validate[ByteRef]
        case JsSuccess("booleanRef", _) => json.validate[BooleanRef]
        case JsSuccess("charRef", _) => json.validate[CharRef]
        case JsSuccess("stringRef", _) => json.validate[StringRef]
        case JsSuccess(_, _) => JsError("Invalid Query Value")
        case x: JsError => x
      }
    }

    override def writes(o: ValueRef): JsValue = {
      val (tpe, json) = o match {
        case x: DoubleRef => ("doubleRef", Json.toJson(x)(doubleRefQueryFormat))
        case x: FloatRef => ("floatRef", Json.toJson(x)(floatRefQueryFormat))
        case x: LongRef => ("longRef", Json.toJson(x)(longRefQueryFormat))
        case x: IntRef => ("intRef", Json.toJson(x)(intRefQueryFormat))
        case x: ShortRef => ("shortRef", Json.toJson(x)(shortRefQueryFormat))
        case x: ByteRef => ("byteRef", Json.toJson(x)(byteRefQueryFormat))
        case x: BooleanRef => ("booleanRef", Json.toJson(x)(booleanRefQueryFormat))
        case x: CharRef => ("charRef", Json.toJson(x)(charRefQueryFormat))
        case x: StringRef => ("stringRef", Json.toJson(x)(stringRefQueryFormat))
      }

      val map = Map("type" -> JsString(tpe), "fragment" -> json)

      JsObject(map)
    }
  }
  implicit val eqPredicateQueryFormat = Json.format[Query.Predicate.Eq]
  implicit val andPredicateQueryFormat = new Format[Predicate.And] {
    val pqf: Format[Predicate] = createPredicateQueryFormat
    val seqpf: Format[Seq[Predicate]] = seqFormat(pqf)
    override def reads(json: JsValue): JsResult[Predicate.And] =
      seqpf.reads(json).map(Predicate.And)

    override def writes(o: Predicate.And): JsValue =
      seqpf.writes(o.predicates)
  }

  implicit val orPredicateQueryFormat = new Format[Predicate.Or] {
    val pqf: Format[Predicate] = createPredicateQueryFormat
    val seqpf: Format[Seq[Predicate]] = seqFormat(pqf)
    override def reads(json: JsValue): JsResult[Predicate.Or] =
      seqpf.reads(json).map(Predicate.Or)

    override def writes(o: Predicate.Or): JsValue = {
      seqpf.writes(o.predicates)
    }
  }

  def createPredicateQueryFormat: Format[Query.Predicate] = new Format[Predicate] {
    override def reads(json: JsValue): JsResult[Predicate] = {
      (json \ "type").validate[String] match {
        case JsSuccess("eqPredicate", _) => json.validate[Predicate.Eq]
        case JsSuccess("orPredicate", _) => json.validate[Predicate.Or]
        case JsSuccess("andPredicate", _) => json.validate[Predicate.And]
        case JsSuccess(_, _) => JsError("Invalid predicate")
        case x: JsError => x
      }
    }

    override def writes(o: Predicate): JsValue = {
      val (tpe, json) = o match {
        case x: Predicate.Eq => ("eqPredicate", Json.toJson(x)(eqPredicateQueryFormat))
        case x: Predicate.Or => ("orPredicate", Json.toJson(x)(orPredicateQueryFormat))
        case x: Predicate.And => ("andPredicate", Json.toJson(x)(andPredicateQueryFormat))
      }
      val map = Map("type" -> JsString(tpe), "fragment" -> json)

      JsObject(map)
    }
  }

  implicit val predicateQueryFormat = createPredicateQueryFormat

  implicit val queryFormat: Format[Query] = new Format[Query] {
    override def reads(json: JsValue): JsResult[Query] = {
      (json \ "type").validate[String] match {
        case JsSuccess("noPredicateQuery", _) => JsSuccess(NoPredicateQuery)
        case JsSuccess("predicateQuery", _) => json.validate[Predicate]
        case JsSuccess(_, _) => JsError("Invalid Query")
        case x: JsError => x
      }
    }

    override def writes(o: Query): JsValue = {
      val (tpe, json) = o match {
        case NoPredicateQuery => ("noPredicateQuery", JsNull)
        case p: Predicate => ("predicateQuery", Json.toJson(p)(predicateQueryFormat))
      }

      val map = Map("type" -> JsString(tpe), "fragment" -> json)

      JsObject(map)
    }
  }

  implicit def signingKeyPairWrites(
      implicit pub: Writes[SigningPublicKey],
      priv: Writes[SigningPrivateKey]): Writes[SigningKeyPair] = Writes { obj =>
    val map = Map(
      "publicKey" -> pub.writes(obj.public),
      "privateKey" -> priv.writes(obj.`private`)
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

      Json.obj("type" -> JsString(tpe), "fragment" -> json)
    }

    override def reads(json: JsValue): JsResult[ChimericTxFragment] = {
      val fragment = json \ "fragment"

      (json \ "type").validate[String] match {
        case JsSuccess("withdrawal", _) => fragment.validate[Withdrawal]
        case JsSuccess("mint", _) => fragment.validate[Mint]
        case JsSuccess("fee", _) => fragment.validate[Fee]
        case JsSuccess("output", _) => fragment.validate[Output]
        case JsSuccess("createCurrency", _) => fragment.validate[CreateCurrency]
        case JsSuccess("deposit", _) => fragment.validate[Deposit]
        case JsSuccess("signatureTxFragment", _) => fragment.validate[SignatureTxFragment]
        case JsSuccess(_, _) => JsError("Invalid ChimericTxFragment")
        case x: JsError => x
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

        (json \ "type").validate[String] match {
          case JsSuccess("mint", _) => fragment.validate[Mint]
          case JsSuccess("fee", _) => fragment.validate[Fee]
          case JsSuccess("output", _) => fragment.validate[Output]
          case JsSuccess("createCurrency", _) => fragment.validate[CreateCurrency]
          case JsSuccess("deposit", _) => fragment.validate[Deposit]
          case JsSuccess("signatureTxFragment", _) => fragment.validate[SignatureTxFragment]
          case JsSuccess(_, _) => JsError("Missing or invalid NonSignableChimericTxFragment")
          case x: JsError => x
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

  implicit val claimDataFormats = Json.format[ClaimData]
  implicit val linkDataFormats = Json.format[LinkData]
  implicit val unlinkDataFormats = Json.format[UnlinkData]
  implicit val endorseDataFormats = Json.format[EndorseData]
  implicit val grantDataFormats = Json.format[GrantData]
  implicit val revokeEndorsementDataFormats = Json.format[RevokeEndorsementData]
  implicit val linkCertificateDataFormat: Format[LinkCertificateData] = new Format[LinkCertificateData] {
    override def writes(o: LinkCertificateData): JsValue = {
      Json.obj(
        "linkingIdentity" -> o.linkingIdentity,
        "pem" -> ByteString(o.pem).utf8String
      )
    }
    override def reads(json: JsValue): JsResult[LinkCertificateData] = {
      for {
        linkingIdentity <- json.\("linkingIdentity").validate[String]
        pem <- json.\("pem").validate[String]
      } yield {
        LinkCertificateData(linkingIdentity, fromHex(pem).utf8String)
      }
    }
  }

  implicit val identityTxDataFormats = Json.format[IdentityTransactionData]

  implicit val requestReads: Reads[CreateIdentityTransactionRequest] = Json.reads[CreateIdentityTransactionRequest]

  implicit val responseWrites: Writes[IdentityTransaction] = Writes[IdentityTransaction] { obj =>
    val tpe = obj match {
      case _: Claim => IdentityTransactionType.Claim
      case _: Link => IdentityTransactionType.Link
      case _: Unlink => IdentityTransactionType.Unlink
      case _: Endorse => IdentityTransactionType.Endorse
      case _: Grant => IdentityTransactionType.Grant
      case _: RevokeEndorsement => IdentityTransactionType.Revoke
      case _: LinkCertificate => IdentityTransactionType.LinkCertificate
    }

    val linkingIdentitySignatureMayBe = obj match {
      case l: Link => Map("linkingIdentitySignature" -> JsString(toCleanHex(l.linkingIdentitySignature.toByteString)))
      case l: LinkCertificate =>
        Map("signatureFromCertificate" -> JsString(toCleanHex(l.signatureFromCertificate.toByteString)))
      case _ => Map.empty[String, JsString]
    }

    val map = Map(
      "type" -> JsString(tpe.toString),
      "data" -> identityTxDataFormats.writes(obj.data),
      "signature" -> JsString(toCleanHex(obj.signature.toByteString))
    )

    JsObject(map ++ linkingIdentitySignatureMayBe)
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

  implicit val nodeIdFormat: Format[NodeId] = new Format[NodeId] {

    override def writes(o: NodeId): JsValue = {
      val hex = toCleanHex(o.id)
      JsString(hex)
    }

    override def reads(json: JsValue): JsResult[NodeId] = {
      json
        .asOpt[String]
        .map { string =>
          Try(NodeId.apply(string))
            .map(JsSuccess(_))
            .getOrElse(JsError("Invalid nodeId"))
        }
        .getOrElse(JsError("Missing nodeId"))
    }
  }

  implicit lazy val singleNodeFormat: Format[SingleNode] = Json.format[SingleNode]
  implicit lazy val setOfNodesFormat: Format[SetOfNodes] = Json.format[SetOfNodes]
  implicit lazy val orFormat: Format[Or] = Json.format[Or]
  implicit lazy val andFormat: Format[And] = Json.format[And]
  implicit lazy val notFormat: Format[Not] = Json.format[Not]

  implicit lazy val destinationDescriptorFormat: Format[DestinationDescriptor] = new Format[DestinationDescriptor] {
    override def writes(o: DestinationDescriptor): JsValue = {
      val (tpe, json) = o match {
        case Everyone => ("everyone", JsObject.empty)
        case x: SingleNode => ("singleNode", Json.toJson(x)(singleNodeFormat))
        case x: SetOfNodes => ("setOfNodes", Json.toJson(x)(setOfNodesFormat))
        case x: Not => ("not", Json.toJson(x)(notFormat))
        case x: And => ("and", Json.toJson(x)(andFormat))
        case x: Or => ("or", Json.toJson(x)(orFormat))
      }

      val map = Map("type" -> JsString(tpe), "obj" -> json)
      JsObject(map)
    }

    override def reads(json: JsValue): JsResult[DestinationDescriptor] = {
      val obj = json \ "obj"
      (json \ "type")
        .asOpt[String]
        .flatMap {
          case "everyone" => Some(Everyone)
          case "singleNode" => obj.asOpt[SingleNode]
          case "setOfNodes" => obj.asOpt[SetOfNodes]
          case "not" => obj.asOpt[Not]
          case "and" => obj.asOpt[And]
          case "or" => obj.asOpt[Or]
          case _ => None
        }
        .map(JsSuccess(_))
        .getOrElse(JsError("Missing or invalid destinationDescriptor"))
    }
  }

  implicit def formatEnvelope[A](implicit format: Format[A]): Format[Envelope[A]] = {
    val builder = (__ \ 'content).format[A] and
      (__ \ 'containerId).format[TableId] and
      (__ \ 'destinationDescriptor).format[DestinationDescriptor]

    builder.apply(Envelope.apply, unlift(Envelope.unapply))
  }

  private val DataItemServiceResponseValidationFormat: Format[DataItemServiceResponse.Validation] =
    Json.format[DataItemServiceResponse.Validation]

  implicit val DataItemServiceResponseWrites: Writes[DataItemServiceResponse] = new Writes[DataItemServiceResponse] {
    override def writes(o: DataItemServiceResponse): JsValue = o match {
      case DataItemServiceResponse.DIUnit => JsObject.empty
      case v: DataItemServiceResponse.Validation => DataItemServiceResponseValidationFormat.writes(v)
    }
  }

}
