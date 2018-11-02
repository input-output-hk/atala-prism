package io.iohk.cef.data
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.codecs.nio._

class Table(tableStorage: TableStorage) {

  def validate[I](dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Boolean = {
    val signatureValidation = validateSignatures(dataItem)
    dataItem().isRight && signatureValidation.forall(_._2)
  }

  def insert[I](dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Unit] = {
    dataItem().flatMap { _ =>
      val signatureValidation = validateSignatures(dataItem)
      val validationErrors = signatureValidation.filter(!_._2).map(_._1)
      if (validationErrors.nonEmpty) {
        val error = new InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(dataItem))
      }
    }
  }

  def delete[I](dataItem: DataItem[I], deleteSignature: Signature)(
      implicit itemSerializable: NioEncDec[I],
      actionSerializable: NioEncDec[DataItemAction[I]]): Either[ApplicationError, Unit] = {
    dataItem().flatMap { _ =>
      import io.iohk.cef.codecs.nio.auto._
      val serializedAction = actionSerializable.encode(DataItemAction.Delete(dataItem))
      val signatureValidation =
        dataItem.owners.map(owner => isValidSignature(serializedAction, deleteSignature, owner.key))
      val validSignature = signatureValidation.find(identity)
      if (dataItem.owners.nonEmpty && validSignature.isEmpty) {
        val error = new OwnerMustSignDelete(dataItem)
        Left(error)
      } else {
        Right(tableStorage.delete(dataItem.id))
      }
    }
  }

  private def validateSignatures[I](dataItem: DataItem[I])(
      implicit itemSerializable: NioEncDec[I]): Seq[(Signature, Boolean)] = {
    val serializedDataItemData = itemSerializable.encode(dataItem.data)
    val signatureValidation = dataItem.witnesses.map {
      case Witness(key, signature) =>
        import io.iohk.cef.codecs.nio.auto._
        (signature, isValidSignature(serializedDataItemData, signature, key))
    }
    signatureValidation
  }
}
