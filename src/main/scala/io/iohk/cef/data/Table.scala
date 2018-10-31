package io.iohk.cef.data
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError

// TODO will probably want to type Table[T] since tableStorage should be typed.
class Table(tableStorage: TableStorage) {

  def insert[I: NioEncoder](dataItem: DataItem[I]): Either[ApplicationError, Unit] = {
    dataItem().flatMap { _ =>
      val signatureValidation = DataItem.validateSignatures(dataItem)
      val validationErrors = signatureValidation.filter(!_._2).map(_._1)
      if (validationErrors.nonEmpty) {
        val error = new InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(dataItem))
      }
    }
  }

  def delete[I](dataItem: DataItem[I], deleteSignature: Signature): Either[ApplicationError, Unit] = {
    dataItem().flatMap { _ =>
      val signatureValidation =
        dataItem.owners.map(owner => isValidSignature(dataItem, deleteSignature, owner.key))

      val validSignature = signatureValidation.find(identity)

      if (dataItem.owners.nonEmpty && validSignature.isEmpty) {
        val error = new OwnerMustSignDelete(dataItem)
        Left(error)
      } else {
        Right(tableStorage.delete(dataItem))
      }
    }
  }
}
