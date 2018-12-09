package io.iohk.cef.data
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.error.{InvalidSignaturesError, OwnerMustSignDelete}
import io.iohk.cef.data.query.{Query, QueryError}
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.error.ApplicationError

import scala.reflect.runtime.universe.TypeTag

class Table[I: NioCodec: TypeTag](val tableId: TableId, tableStorage: TableStorage[I]) {

  def validate(dataItem: DataItem[I])(implicit canValidate: CanValidate[DataItem[I]]): Boolean = {
    val signatureValidation = validateSignatures(dataItem)
    canValidate.validate(dataItem).isRight && signatureValidation.forall(_._2)
  }

  def select(query: Query): Either[QueryError, Seq[DataItem[I]]] = {
    tableStorage.select(query)
  }

  def insert(dataItem: DataItem[I])(implicit canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
    canValidate.validate(dataItem).flatMap { _ =>
      val validationErrors = validateSignatures(dataItem).filter(!_._2).map(_._1)
      if (validationErrors.nonEmpty) {
        val error = InvalidSignaturesError(dataItem, validationErrors)
        Left(error)
      } else {
        Right(tableStorage.insert(dataItem))
      }
    }
  }

  def delete(dataItemId: DataItemId, deleteSignature: Signature)(
      implicit canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] = {
    for {
      dataItem <- tableStorage.selectSingle(dataItemId)
      _ <- canValidate.validate(dataItem)
    } yield {
      val signatureValidation =
        dataItem.owners.map(owner => isValidSignature(DeleteSignatureWrapper(dataItem), deleteSignature, owner.key))

      val validSignature = signatureValidation.find(identity)

      if (dataItem.owners.nonEmpty && validSignature.isEmpty) {
        val error = OwnerMustSignDelete(dataItem)
        Left(error)
      } else {
        Right(tableStorage.delete(dataItem))
      }
    }
  }

  private def validateSignatures(dataItem: DataItem[I]): Seq[(Signature, Boolean)] = {
    val serializedDataItemData = NioCodec[I].encode(dataItem.data)
    val signatureValidation = dataItem.witnesses.map {
      case Witness(key, signature) =>
        import io.iohk.cef.codecs.nio.auto._
        (signature, isValidSignature(serializedDataItemData, signature, key))
    }
    signatureValidation
  }
}
