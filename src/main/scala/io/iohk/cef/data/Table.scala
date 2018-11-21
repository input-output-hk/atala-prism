package io.iohk.cef.data
import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.data.error.{InvalidSignaturesError, OwnerMustSignDelete}
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.error.ApplicationError
import scala.reflect.runtime.universe.TypeTag

// TODO will probably want to type Table[T] since tableStorage should be typed.
class Table(tableStorage: TableStorage) {

  def validate[I](dataItem: DataItem[I])(
      implicit itemSerializable: NioEncDec[I],
      canValidate: CanValidate[DataItem[I]]): Boolean = {
    val signatureValidation = validateSignatures(dataItem)
    canValidate.validate(dataItem).isRight && signatureValidation.forall(_._2)
  }

  def select[I: NioEncDec: TypeTag](tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    tableStorage.select(tableId, query)
  }

  def insert[I](tableId: TableId, dataItem: DataItem[I])(
      implicit codec: NioEncDec[I],
      typeTag: TypeTag[I],
      canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
    canValidate.validate(dataItem).flatMap { _ =>
      val validationErrors = validateSignatures(dataItem).filter(!_._2).map(_._1)
      if (validationErrors.nonEmpty) {
        val error = InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(tableId, dataItem))
      }
    }
  }

  def delete[I](tableId: TableId, dataItemId: DataItemId, deleteSignature: Signature)(
      implicit dataSerializable: NioEncDec[I],
      typeTag: TypeTag[I],
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
        val error = OwnerMustSignDelete(dataItem)
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
