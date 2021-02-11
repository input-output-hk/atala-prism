package io.iohk.atala.prism.credentials.content

import io.iohk.atala.prism.credentials.content.CredentialContent.JsonFields
import io.iohk.atala.prism.identity.DID

import io.iohk.atala.prism.credentials.content.CredentialContent._

case class CredentialContent(fields: Fields) {

  /**
    * Access credential field by name.
    */
  def apply(field: String): Either[CredentialContentException, Value] = getValue(field)

  /**
    * Access credential field by name.
    */
  def getValue(field: String): Either[CredentialContentException, Value] = {
    val path = field.split('.')
    path.drop(1).foldLeft(fields.find(_.name == path(0)).map(_.value)) {
      case (Some(sf: SubFields), key) => sf.value.find(_.name == key).map(_.value)
      case _ => None
    } match {
      case Some(value) => Right(value)
      case None => Left(FieldNotFoundException(s"Field not found: $field"))
    }
  }

  def getString(field: String): Either[CredentialContentException, String] = {
    getValue(field).flatMap {
      case StringValue(value) => Right(value)
      case _ => Left(WrongTypeException(s"$field is not a String."))
    }
  }

  def getInt(field: String): Either[CredentialContentException, Int] = {
    getValue(field).flatMap {
      case IntValue(value) => Right(value)
      case _ => Left(WrongTypeException(s"$field is not an Int."))
    }
  }

  def getBoolean(field: String): Either[CredentialContentException, Boolean] = {
    getValue(field).flatMap {
      case BooleanValue(value) => Right(value)
      case _ => Left(WrongTypeException(s"$field is not an Boolean."))
    }
  }

  def getSeq(field: String): Either[CredentialContentException, Seq[Any]] = {
    getValue(field).flatMap {
      case SeqValue(values) => Right(values.map(getValue))
      case _ => Left(WrongTypeException(s"$field is not an Int."))
    }
  }

  def getSubFields(field: String): Either[CredentialContentException, IndexedSeq[(String, Any)]] = {
    getValue(field).flatMap {
      case SubFields(fields) => Right(fields.map(f => (f.name, getValue(f.value))))
      case _ => Left(WrongTypeException(s"$field is not an Int."))
    }
  }

  // Predefined fields
  val issuerDid: Either[CredentialContentException, DID] =
    getString(JsonFields.IssuerDid.field).flatMap(did =>
      DID.fromString(did).toRight(WrongTypeException("IssuerDid is not a string."))
    )
  val issuanceKeyId: Either[CredentialContentException, String] = getString(JsonFields.IssuanceKeyId.field)
  val credentialSubject: Either[CredentialContentException, String] = getString(JsonFields.CredentialSubject.field)

  /**
    * Get the underlying value from [[Value]] class.
    */
  private def getValue(value: Value): Any = {
    value match {
      case StringValue(value) => value
      case IntValue(value) => value
      case BooleanValue(value) => value
      case SeqValue(values) => values.map(getValue)
      case SubFields(fields) => fields.map(f => (f.name, getValue(f.value)))
    }
  }
}
object CredentialContent {
  def apply(fields: Field*): CredentialContent = CredentialContent(IndexedSeq(fields: _*))
  def empty: CredentialContent = CredentialContent(IndexedSeq.empty)

  sealed trait Value extends Any { def value: Any }
  case class StringValue(value: String) extends AnyVal with Value
  case class IntValue(value: Int) extends AnyVal with Value
  case class BooleanValue(value: Boolean) extends AnyVal with Value
  case class SeqValue(value: Values) extends AnyVal with Value
  case class SubFields(value: Fields) extends AnyVal with Value

  case class Field(name: String, value: Value)

  type Values = Seq[Value]
  def Values(xs: Value*): Values = Seq(xs: _*)

  type Fields = IndexedSeq[Field]
  def Fields(xs: Field*): Fields = IndexedSeq(xs: _*)

  sealed class CredentialContentException(message: String) extends Exception(message)
  case class FieldNotFoundException(message: String) extends CredentialContentException(message)
  case class WrongTypeException(message: String) extends CredentialContentException(message)

  object JsonFields {
    sealed abstract class Field(val field: String)
    case object CredentialType extends Field("type")
    case object Issuer extends Field("issuer")
    case object IssuerDid extends Field("id")
    case object IssuerName extends Field("name")
    case object IssuanceKeyId extends Field("keyId")
    case object IssuanceDate extends Field("issuanceDate")
    case object ExpiryDate extends Field("expiryDate")
    case object CredentialSubject extends Field("credentialSubject")
  }
}
