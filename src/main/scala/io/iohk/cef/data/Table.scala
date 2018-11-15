package io.iohk.cef.data
import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.data.error.{InvalidSignaturesError, OwnerMustSignDelete}
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.error.ApplicationError

// TODO will probably want to type Table[T] since tableStorage should be typed.
class Table(tableStorage: TableStorage) {

  def validate[I](dataItem: DataItem[I])(
      implicit itemSerializable: NioEncDec[I],
      canValidate: CanValidate[DataItem[I]]): Boolean = {
    val signatureValidation = validateSignatures(dataItem)
    canValidate.validate(dataItem).isRight && signatureValidation.forall(_._2)
  }

  def insert[I](tableId: TableId, dataItem: DataItem[I])(
      implicit itemSerializable: NioEncDec[I],
      canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
    canValidate.validate(dataItem).flatMap { _ =>
      val validationErrors = validateSignatures(dataItem).filter(!_._2).map(_._1)
      if (validationErrors.nonEmpty) {
        val error = new InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(tableId, dataItem))
      }
    }
  }

  def delete[I](tableId: TableId, dataItemId: DataItemId, deleteSignature: Signature)(
      implicit dataSerializable: NioEncDec[I],
      dataItemSerializable: NioEncDec[DataItem[I]],
      actionSerializable: NioEncDec[DeleteSignatureWrapper[I]],
      canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
    for {
      dataItem <- tableStorage.selectSingle[I](tableId, dataItemId)
      _ <- canValidate.validate(dataItem)
    } yield {
      val signatureValidation =
        dataItem.owners.map(owner => isValidSignature(DeleteSignatureWrapper(dataItem), deleteSignature, owner.key))

      val validSignature = signatureValidation.find(identity)

      if (dataItem.owners.nonEmpty && validSignature.isEmpty) {
        val error = new OwnerMustSignDelete(dataItem)
        Left(error)
      } else {
        Right(tableStorage.delete(tableId, dataItem))
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
