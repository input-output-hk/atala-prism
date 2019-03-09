package io.iohk.cef.frontend.models

import io.iohk.cef.utils.NonEmptyList
import play.api.libs.json._

trait CommonCodecs {

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

  implicit def nonEmptyListFormat[T](implicit formatT: Format[T]): Format[NonEmptyList[T]] = {
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
  }
}
