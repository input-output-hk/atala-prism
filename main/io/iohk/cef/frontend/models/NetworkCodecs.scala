package io.iohk.cef.frontend.models

import io.iohk.cef.data.TableId
import io.iohk.cef.utils.HexStringCodec
import io.iohk.network._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

trait NetworkCodecs {

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

  implicit val nodeIdFormat: Format[NodeId] = new Format[NodeId] {

    override def writes(o: NodeId): JsValue = {
      val hex = HexStringCodec.toHexString(o.id)
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
}
