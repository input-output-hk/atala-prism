package io.iohk.cef.data
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError

class Table[I <: DataItem](tableId: TableId, tableStorage: TableStorage)(
    implicit itemSerializable: ByteStringSerializable[I]) {

  def validate(dataItem: I): Boolean = {
    val signatureValidation = validateSignatures(dataItem)
    dataItem.validate.isRight && signatureValidation.forall(_._2)
  }

  def insert(dataItem: I): Either[ApplicationError, Unit] = {
    dataItem.validate.flatMap { _ =>
      val signatureValidation = validateSignatures(dataItem)
      val validationErrors = signatureValidation.filter(!_._2).map(_._1)
      if (!validationErrors.isEmpty) {
        val error = new InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(tableId, dataItem))
      }
    }
  }

  def delete(dataItem: I, deleteSignature: Signature)(
      implicit actionSerializable: ByteStringSerializable[DataItemAction[I]]): Either[ApplicationError, Unit] = {
    dataItem.validate.flatMap { _ =>
      val serializedAction = actionSerializable.encode(DataItemAction.Delete(dataItem))
      val signatureValidation =
        dataItem.owners.map(ownerKey => isValidSignature(serializedAction, deleteSignature, ownerKey))
      val validSignature = signatureValidation.find(identity)
      if (!dataItem.owners.isEmpty && validSignature.isEmpty) {
        val error = new OwnerMustSignDelete(dataItem)
        Left(error)
      } else {
        Right(tableStorage.delete(tableId, dataItem))
      }
    }
  }

  private def validateSignatures(dataItem: I): Seq[(Signature, Boolean)] = {
    val serializedDataItem = itemSerializable.encode(dataItem)
    val signatureValidation = dataItem.witnesses.map {
      case (key, signature) =>
        (signature, isValidSignature(serializedDataItem, signature, key))
    }
    signatureValidation
  }
}
