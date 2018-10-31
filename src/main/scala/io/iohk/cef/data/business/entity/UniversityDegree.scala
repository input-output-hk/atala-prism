package io.iohk.cef.data.business.entity
import java.time.LocalDate

import io.iohk.cef.crypto._
import io.iohk.cef.data._

case class UniversityDegreeData(universityName: String, degree: String, studentName: String, date: LocalDate)

object UniversityDegreeData {

  implicit def UniversityDegreeValidation(
      implicit publicKeyStore: Map[String, SigningPublicKey],
      serializable: NioEncoder[UniversityDegreeData]): CanValidate[DataItem[UniversityDegreeData]] = {
    t: DataItem[UniversityDegreeData] =>
      {
        def getSigningPublicKey(name: String): Either[DataItemError, SigningPublicKey] = {
          publicKeyStore.get(name) match {
            case Some(key) => Right(key)
            case _ => Left(UniversityWitnessNotProvidedError(t.data.universityName, t.witnesses.head, t.id))
          }
        }

        getSigningPublicKey(t.data.universityName).map { key =>
          if (isValidSignature(t.data, t.witnesses.head.signature, t.witnesses.head.key)) {
            Right(Unit)
          } else {
            Left(InvalidUniversitySignatureError(t.data.universityName, t.id))
          }
        }
      }
  }
}

case class InvalidUniversitySignatureError(universityName: String, id: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university ${universityName} and data item ${id}"
}
case class UniversityWitnessNotProvidedError(universityName: String, witness: Witness, id: String)
    extends DataItemError {
  override def toString: String =
    s"Expected the university's public key to match ${witness.key},but the public key provided is not from the university ${universityName} in data item ${id} "
}

