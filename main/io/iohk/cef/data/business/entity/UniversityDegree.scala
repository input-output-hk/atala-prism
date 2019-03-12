package io.iohk.cef.data.business.entity

import java.time.LocalDate

import io.iohk.cef.data.error.DataItemError
import io.iohk.cef.data.{CanValidate, DataItem, Witness}
import io.iohk.codecs.nio._
import io.iohk.crypto._

case class UniversityDegreeData(universityName: String, degree: String, studentName: String, date: LocalDate)

object UniversityDegreeData {

  implicit def universityDegreeValidation(
      implicit publicKeyStore: Map[String, SigningPublicKey],
      serializable: NioCodec[UniversityDegreeData]
  ): CanValidate[DataItem[UniversityDegreeData]] = { dataItem: DataItem[UniversityDegreeData] =>
    mandatoryCheck(dataItem).flatMap { d =>
      getSigningPublicKey(d).flatMap { key =>
        if (isValidSignature(d.data, d.witnesses.head.signature, key)) {
          Right(Unit)
        } else {
          Left(InvalidUniversitySignatureError(d.data.universityName))
        }
      }

    }

  }

  private def getSigningPublicKey(
      dataItem: DataItem[UniversityDegreeData]
  )(implicit publicKeyStore: Map[String, SigningPublicKey]): Either[DataItemError, SigningPublicKey] = {
    publicKeyStore.get(dataItem.data.universityName) match {
      case Some(key) => Right(key)
      case _ =>
        Left(UniversityPublicKeyIsUnknown(dataItem.data.universityName, dataItem.witnesses.head))
    }
  }
  private def mandatoryCheck(
      dataItem: DataItem[UniversityDegreeData]
  ): Either[DataItemError, DataItem[UniversityDegreeData]] = {
    if (dataItem.witnesses.isEmpty) Left(NoWitnessProvided(dataItem.data.universityName))
    else if (dataItem.owners.isEmpty) Left(NoOwnerProvided(dataItem.data.universityName))
    else Right(dataItem)
  }
}

case class InvalidUniversitySignatureError(universityName: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university $universityName"
}

case class NoWitnessProvided(universityName: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university $universityName"
}

case class NoOwnerProvided(universityName: String) extends DataItemError {
  override def toString: String = s"Invalid signature was provided for university $universityName"
}

case class UniversityPublicKeyIsUnknown(universityName: String, witness: Witness) extends DataItemError {
  override def toString: String =
    s"Expected the university's public key to match ${witness.key},but the public key provided is not from the university $universityName"
}
