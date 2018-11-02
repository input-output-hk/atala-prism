package io.iohk.cef.data.business.entity
import java.time.LocalDate

import io.iohk.cef.crypto._
import io.iohk.cef.data.{DataItem, DataItemError, Owner, Witness}
import io.iohk.cef.codecs.nio._

case class UniversityDegreeData(universityName: String, degree: String, studentName: String, date: LocalDate)

/**
  * @param id
  * @param data university Degree Data
  * @param universityWitness
  * @param publicKeyStore publicKeyStore map with universityName <- publicKey.
  *                       This is tactical solution since we don't have link with Identity Transaction
  * @param serializable
  */
case class UniversityDegree(
    id: String,
    data: UniversityDegreeData,
    universityWitness: Witness,
    publicKeyStore: Map[String, SigningPublicKey])(implicit serializable: NioEncoder[UniversityDegreeData])
    extends DataItem[UniversityDegreeData] {

  /**
    * Users/entities with permission to eliminate this data item
    *
    * @return
    */
  override def owners: Seq[Owner] = {
    getSigningPublicKey(data.universityName) match {
      case Right(o) => List(Owner(o))
      case _ => List()
    }
  }

  override def apply(): Either[DataItemError, Unit] = {

    getSigningPublicKey(data.universityName).map { key =>
      if (isValidSignature(data, universityWitness.signature, key)) {
        Right(Unit)
      } else
        Left(InvalidUniversitySignatureError(data.universityName, id))
    }

  }

  /**
    * Users/entities that witnessed this item and signed it
    *
    * @return
    */
  override def witnesses: Seq[Witness] = List(universityWitness)

  private def getSigningPublicKey(name: String): Either[DataItemError, SigningPublicKey] = {
    publicKeyStore.get(name) match {
      case Some(key) => Right(key)
      case _ => Left(UniversityWitnessNotProvidedError(data.universityName, universityWitness, id))
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
