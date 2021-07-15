package io.iohk.atala.prism.kycbridge.models.identityMind

import io.circe.Decoder
import io.circe.Encoder

final case class PostConsumerResponse(
    mtid: String, // The transaction id for this KYC. This id should be provided on subsequent updates to the KYC
    user: String, // The current reputation of the user involved in this transaction,
    upr: Option[
      String
    ], // The previous reputation of the User, that is, the reputation of the user the last time that it was evaluated
    frn: Option[String], // The name of the fraud rule that fired
    frp: Option[String], // Result of fraud evaluation
    frd: Option[String], // The description of the fraud rule that fired
    arpr: Option[String] // Result of automated review evaluation
)

final case class EdnaScoreCardEntry(
    test: Option[String],
    details: Option[String]
)

final case class EdnaScoreCard(
    etr: List[EdnaScoreCardEntry]
)

sealed abstract class ConsumerResponseState(val state: String)
object ConsumerResponseState {
  case object Accept extends ConsumerResponseState("A")
  case object Deny extends ConsumerResponseState("D")
  case object Review extends ConsumerResponseState("R")

  implicit val decodeConsumerResponseState: Decoder[ConsumerResponseState] = Decoder[String].emap {
    case Accept.state => Right(Accept)
    case Deny.state => Right(Deny)
    case Review.state => Right(Review)
    case other => Left(s"Invalid state: $other")
  }

  implicit val encodeConsumerResponseState: Encoder[ConsumerResponseState] = Encoder[String].contramap {
    case Accept => Accept.state
    case Deny => Deny.state
    case Review => Review.state
  }
}

final case class GetConsumerResponse(
    mtid: String,
    state: ConsumerResponseState,
    ednaScoreCard: EdnaScoreCard
) {

  /**
    * Return first natching data field.
    */
  def getDataField(field: GetConsumerResponse.FieldName): Option[EdnaScoreCardEntry] =
    getDataField(field.name)

  /**
    * Return first natching data field.
    */
  def getDataField(fieldName: String): Option[EdnaScoreCardEntry] =
    Option(ednaScoreCard.etr).flatMap(_.find(_.test.contains(fieldName)))
}

object GetConsumerResponse {

  // dv:0  - test if the service was accessed successfully
  // dv:1 – test if the image was able to be processed as a document
  // dv:2 – doc type match (not relevant)
  // dv:3 – test if the document appears authentic
  // dv:4 – test if the face image was processed as a face
  // dv:5 – face match
  // dv:6 – liveness processed (not relevant)
  // dv:7 – liveness verified (not relevant)
  // dv:8 – name match (not relevant)
  // dv:9 – dob match (not relevant)
  // dv:10 – address match (not relevant)
  // dv:11 – doc expired test
  // dv:12 – service provider (will always show Mitek)
  // dv:13 – extracted name
  // dv:14 – extracted address
  // dv:15 – extracted dob
  // dv:16 – extracted document number
  // dv:17 – extracted expiration date
  // dv:18 – Country of ID
  // dv:19 – Type of document
  // dv:20 – document country match (not relevant)
  // dv:21 – result descisive test
  // dv:22 – extracted document number 2
  // dv:23 – liveness score (not relevant)
  // dv:24 – manual service accessed (will always be ‘true’)

  sealed abstract class FieldName(val name: String)
  case object Name extends FieldName("dv:13")
  case object Address extends FieldName("dv:14")
  case object DocumentNumber extends FieldName("dv:16")
  case object ExpirationDate extends FieldName("dv:17")
  case object CountryOfId extends FieldName("dv:18")
  case object TypeOfDocument extends FieldName("dv:19")

  val INVALID_ADDRESS = "Address could NOT be extracted from the document."

}
