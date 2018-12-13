package io.iohk.cef.frontend.models

import enumeratum.EnumEntry._
import enumeratum._
import io.iohk.cef.ledger.identity
import io.iohk.cef.ledger.identity._

/**
  * {{{
  *
  *   # Some setup
  *   >>> import spray.json._
  *
  *   # Convert a value into json
  *   >>> IdentityTransactionType.Link.toJson
  *   "link"
  *
  *   # Recover a value from a json
  *   >>> """"link"""".parseJson.convertTo[IdentityTransactionType]
  *   Link
  *
  *   # something goes wrong
  *   >>> try {
  *   ...   """"Link"""".parseJson.convertTo[IdentityTransactionType]
  *   ... } catch {
  *   ...   case t: Throwable => t.getMessage
  *   ... }
  *   Expected one of 'claim, link, unlink, endorse, revoke, grant, linkcertificate', but got 'Link' instead
  * }}}
  */
sealed abstract class IdentityTransactionType extends EnumEntry with Lowercase

object IdentityTransactionType extends Enum[IdentityTransactionType] with SprayJsonEnum[IdentityTransactionType] {

  val values = findValues

  case object Claim extends IdentityTransactionType
  case object Link extends IdentityTransactionType
  case object Unlink extends IdentityTransactionType
  case object Endorse extends IdentityTransactionType
  case object Revoke extends IdentityTransactionType
  case object Grant extends IdentityTransactionType
  case object LinkCertificate extends IdentityTransactionType

  def of(it: IdentityTransaction): IdentityTransactionType =
    it match {
      case _: identity.Claim => Claim
      case _: identity.Link => Link
      case _: identity.Unlink => Unlink
      case _: identity.Endorse => Endorse
      case _: identity.RevokeEndorsement => Revoke
      case _: identity.Grant => Grant
      case _: identity.LinkCertificate => LinkCertificate
    }
}
