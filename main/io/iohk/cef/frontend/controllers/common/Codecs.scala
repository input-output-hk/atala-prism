package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.data._
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
import io.iohk.cef.frontend.PlayJson
import io.iohk.cef.frontend.models._
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.utils.NonEmptyList
import io.iohk.crypto._
import play.api.libs.json._

object Codecs extends PlayJson.Formats with ChimericCodecs with IdentityCodecs with NetworkCodecs {

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

  implicit val witnessFormat: Format[Witness] = Json.format[Witness]

  implicit val ownerFormat: Format[Owner] = Json.format[Owner]

  implicit def dataItemFormat[T](implicit tFormat: Format[T]): Format[DataItem[T]] = Json.format[DataItem[T]]

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
