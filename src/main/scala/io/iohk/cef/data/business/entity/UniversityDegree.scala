package io.iohk.cef.data.business.entity
import java.time.LocalDate

import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.data.{CanValidate, DataItem, Witness}
import io.iohk.cef.data.error.DataItemError
import io.iohk.cef.data.query.Queriable

case class UniversityDegreeData(universityName: String, degree: String, studentName: String, date: LocalDate)

object UniversityDegreeData {

  implicit val queriableUniversityDegree: Queriable[UniversityDegreeData] = new Queriable[UniversityDegreeData] {
    override val fieldCount: Int = 4
    override def applyPredicate(t: UniversityDegreeData, index: Int, value: String): Boolean = index match {
      case 0 => t.universityName == value
      case 1 => t.degree == value
      case 2 => t.studentName == value
      case 3 => t.date == LocalDate.parse(value)
      case _ => throw new IllegalArgumentException("Invalid index provided on query")
    }
  }

  implicit def universityDegreeValidation(
      implicit publicKeyStore: Map[String, SigningPublicKey],
      serializable: NioEncDec[UniversityDegreeData]): CanValidate[DataItem[UniversityDegreeData]] = {
    dataItem: DataItem[UniversityDegreeData] =>
      mandatoryCheck(dataItem).flatMap { d =>
        getSigningPublicKey(d).flatMap { key =>
          if (isValidSignature(d.data, d.witnesses.head.signature, key)) {
            Right(Unit)
          } else {
            Left(InvalidUniversitySignatureError(d.data.universityName, d.id))
          }
        }

      }

  }

  private def getSigningPublicKey(dataItem: DataItem[UniversityDegreeData])(
      implicit publicKeyStore: Map[String, SigningPublicKey]): Either[DataItemError, SigningPublicKey] = {
    publicKeyStore.get(dataItem.data.universityName) match {
      case Some(key) => Right(key)
      case _ => Left(UniversityPublicKeyIsUnknown(dataItem.data.universityName, dataItem.witnesses.head, dataItem.id))
    }
  }
  private def mandatoryCheck(
      dataItem: DataItem[UniversityDegreeData]): Either[DataItemError, DataItem[UniversityDegreeData]] = {
    if (dataItem.witnesses.isEmpty) Left(NoWitnessProvided(dataItem.data.universityName, dataItem.id))
    else if (dataItem.owners.isEmpty) Left(NoOwnerProvided(dataItem.data.universityName, dataItem.id))
    else Right(dataItem)
  }
}

case class InvalidUniversitySignatureError(universityName: String, id: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university ${universityName} and data item ${id}"
}

case class NoWitnessProvided(universityName: String, id: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university ${universityName} and data item ${id}"
}

case class NoOwnerProvided(universityName: String, id: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university ${universityName} and data item ${id}"
}

case class UniversityPublicKeyIsUnknown(universityName: String, witness: Witness, id: String) extends DataItemError {
  override def toString: String =
    s"Expected the university's public key to match ${witness.key},but the public key provided is not from the university ${universityName} in data item ${id} "
}
