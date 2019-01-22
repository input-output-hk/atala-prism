package io.iohk.cef.frontend.controllers.common

import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.data.query.DataItemQuery._
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
import io.iohk.cef.data.query.{DataItemQuery, Field, Value => ValueRef}
import io.iohk.cef.utils.NonEmptyList
import io.iohk.cef.data._
import io.iohk.cef.frontend.PlayJson
import io.iohk.cef.frontend.models._
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.identity._
import io.iohk.cef.network.NodeId
import io.iohk.cef.transactionservice._
import io.iohk.cef.utils.ByteStringExtension
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

object Codecs extends PlayJson.Formats with LowerPriorityCodecs {

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

  private def formatWrappedT[Wrapped: Format, Wrapper](
      unwrap: Wrapper => Wrapped,
      wrap: Wrapped => Wrapper
  ): Format[Wrapper] = new Format[Wrapper] {
    override def reads(json: JsValue): JsResult[Wrapper] = {
      json
        .validate[Wrapped]
        .map(wrap)
    }

    override def writes(o: Wrapper): JsValue = {
      Json.toJson(unwrap(o))
    }
  }

  implicit val fieldQueryFormat: Format[Field] = formatWrappedT[Int, Field](x => x.index, x => Field(x))
  implicit val doubleRefQueryFormat: Format[DoubleRef] =
    formatWrappedT[Double, DoubleRef](x => x.value, x => DoubleRef(x))
  implicit val floatRefQueryFormat: Format[FloatRef] = formatWrappedT[Float, FloatRef](x => x.value, x => FloatRef(x))
  implicit val longRefQueryFormat: Format[LongRef] = formatWrappedT[Long, LongRef](x => x.value, x => LongRef(x))
  implicit val intRefQueryFormat: Format[IntRef] = formatWrappedT[Int, IntRef](x => x.value, x => IntRef(x))
  implicit val shortRefQueryFormat: Format[ShortRef] = formatWrappedT[Short, ShortRef](x => x.value, x => ShortRef(x))
  implicit val byteRefQueryFormat: Format[ByteRef] = formatWrappedT[Byte, ByteRef](x => x.value, x => ByteRef(x))
  implicit val booleanRefQueryFormat: Format[BooleanRef] =
    formatWrappedT[Boolean, BooleanRef](x => x.value, x => BooleanRef(x))
  implicit val stringRefQueryFormat: Format[StringRef] =
    formatWrappedT[String, StringRef](x => x.value, x => StringRef(x))
  implicit val charRefQueryFormat: Format[CharRef] = new Format[CharRef] {
    override def reads(json: JsValue): JsResult[CharRef] =
      json
        .validate[String]
        .filter(JsError("Invalid char length"))(_.size == 1)
        .map(_.charAt(0))

    override def writes(o: CharRef): JsValue = {
      JsString(o.value.toString)
    }
  }

  implicit val valueQueryFormat: Format[ValueRef] = new Format[ValueRef] {
    override def reads(json: JsValue): JsResult[ValueRef] = {
      val value = json \ "value"
      (json \ "type").validate[String] match {
        case JsSuccess("doubleRef", _) => value.validate[DoubleRef]
        case JsSuccess("floatRef", _) => value.validate[FloatRef]
        case JsSuccess("longRef", _) => value.validate[LongRef]
        case JsSuccess("intRef", _) => value.validate[IntRef]
        case JsSuccess("shortRef", _) => value.validate[ShortRef]
        case JsSuccess("byteRef", _) => value.validate[ByteRef]
        case JsSuccess("booleanRef", _) => value.validate[BooleanRef]
        case JsSuccess("charRef", _) => value.validate[CharRef]
        case JsSuccess("stringRef", _) => value.validate[StringRef]
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

      Json.obj("type" -> JsString(tpe), "value" -> json)
    }
  }

  implicit val eqPredicateQueryFormat = Json.format[DataItemQuery.Predicate.Eq]
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

  def createPredicateQueryFormat: Format[DataItemQuery.Predicate] = new Format[Predicate] {
    override def reads(json: JsValue): JsResult[Predicate] = {
      val value = json \ "value"
      (json \ "type").validate[String] match {
        case JsSuccess("eqPredicate", _) => value.validate[Predicate.Eq]
        case JsSuccess("orPredicate", _) => value.validate[Predicate.Or]
        case JsSuccess("andPredicate", _) => value.validate[Predicate.And]
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

      Json.obj("type" -> JsString(tpe), "value" -> json)
    }
  }

  implicit val predicateQueryFormat = createPredicateQueryFormat

  implicit val queryFormat: Format[DataItemQuery] = new Format[DataItemQuery] {
    override def reads(json: JsValue): JsResult[DataItemQuery] = {
      val value = json \ "value"
      (json \ "type").validate[String] match {
        case JsSuccess("noPredicateQuery", _) => JsSuccess(NoPredicateDataItemQuery)
        case JsSuccess("predicateQuery", _) => value.validate[Predicate]
        case JsSuccess(_, _) => JsError("Invalid Query")
        case x: JsError => x
      }
    }

    override def writes(o: DataItemQuery): JsValue = {
      val (tpe, json) = o match {
        case NoPredicateDataItemQuery => ("noPredicateQuery", JsNull)
        case p: Predicate => ("predicateQuery", Json.toJson(p)(predicateQueryFormat))
      }

      Json.obj("type" -> JsString(tpe), "value" -> json)
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

  implicit def signingKeyPairWrites(
      implicit pub: Writes[SigningPublicKey],
      priv: Writes[SigningPrivateKey]
  ): Writes[SigningKeyPair] = Writes { obj =>
    val map = Map(
      "publicKey" -> pub.writes(obj.public),
      "privateKey" -> priv.writes(obj.`private`)
    )

    JsObject(map)
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
              Json.toJson(x)(createNonSignableChimericTransactionFragmentFormat)
            )
          case x: CreateSignableChimericTransactionFragment =>
            (
              "createSignableChimericTransactionFragment",
              Json.toJson(x)(createSignableChimericTransactionFragmentFormat)
            )
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

  implicit val transactionTypeReads: Format[IdentityTransactionType] = new Format[IdentityTransactionType] {
    override def reads(json: JsValue): JsResult[IdentityTransactionType] = {
      json.validate[String].flatMap { string =>
        IdentityTransactionType
          .withNameInsensitiveOption(string)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("Invalid transaction type")
          }
      }
    }

    override def writes(o: IdentityTransactionType): JsValue = JsString(o.entryName)
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

  implicit val identityTxDataFormats: OFormat[IdentityTransactionData] = Json.format[IdentityTransactionData]

  implicit val requestReads: OFormat[CreateIdentityTransactionRequest] = Json.format[CreateIdentityTransactionRequest]

  implicit val responseWrites: OWrites[IdentityTransaction] = OWrites[IdentityTransaction] { obj =>
    val signatureWrites = implicitly[Writes[Signature]]
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
      case l: Link => Map("linkingIdentitySignature" -> signatureWrites.writes(l.linkingIdentitySignature))
      case l: LinkCertificate =>
        Map("signatureFromCertificate" -> signatureWrites.writes(l.signatureFromCertificate))
      case _ => Map.empty[String, JsString]
    }

    val map = Map(
      "type" -> JsString(tpe.toString),
      "data" -> identityTxDataFormats.writes(obj.data),
      "signature" -> signatureWrites.writes(obj.signature)
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

  implicit def nonEmptyListFormat[T](implicit formatT: Format[T]): Format[NonEmptyList[T]] =
    new Format[NonEmptyList[T]] {
      override def reads(json: JsValue): JsResult[NonEmptyList[T]] = {
        json
          .validate[List[T]]
          .flatMap { list =>
            NonEmptyList
              .from(list)
              .map(JsSuccess.apply(_))
              .getOrElse {
                JsError.apply("A non-empty list is expected")
              }
          }
      }

      override def writes(o: NonEmptyList[T]): JsValue = {
        Json.toJson(o: List[T])
      }
    }

  implicit val UtxoResultFormat: Format[UtxoResult] = Json.format[UtxoResult]
  implicit val AddressResultFormat: Format[AddressResult] = Json.format[AddressResult]
  implicit val NonceResultFormat: Format[NonceResult] = Json.format[NonceResult]
  implicit val CurrencyQueryFormat: Format[CurrencyQuery] = Json.format[CurrencyQuery]
}

trait LowerPriorityCodecs extends LowestPriorityCodecs {

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

}

trait LowestPriorityCodecs {
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
}
