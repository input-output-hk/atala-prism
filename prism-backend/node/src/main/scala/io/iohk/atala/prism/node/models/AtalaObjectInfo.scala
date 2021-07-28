package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.cardano.TX_METADATA_MAX_SIZE
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata
import io.iohk.atala.prism.protos.node_internal

case class AtalaObjectInfo(
    objectId: AtalaObjectId,
    byteContent: Array[Byte], // Serialization of a io.iohk.atala.prism.protos.node_internal.AtalaObject
    processed: Boolean, // Whether the object has been processed (e.g., DIDs were recognized and stored in DB)
    transaction: Option[TransactionInfo] = None // Blockchain transaction the object was first found in
) {
  lazy val getAndValidateAtalaObject: Option[node_internal.AtalaObject] =
    node_internal.AtalaObject.validate(byteContent).toOption

  lazy val getAtalaBlock: Option[node_internal.AtalaBlock] = {
    for {
      atalaObject <- getAndValidateAtalaObject
      atalaBlock <- atalaObject.blockContent
    } yield atalaBlock
  }

  private def appendAtalaObject(that: AtalaObjectInfo): Option[AtalaObjectInfo] = {
    for {
      thisBlock <- getAtalaBlock
      thatBlock <- that.getAtalaBlock
    } yield {
      val mergedBlock = node_internal.AtalaBlock(thisBlock.version, thisBlock.operations ++ thatBlock.operations)
      val obj = node_internal
        .AtalaObject(
          blockOperationCount = mergedBlock.operations.size
        )
        .withBlockContent(mergedBlock)
      AtalaObjectInfo(AtalaObjectId.of(obj), obj.toByteArray, processed = false, None)
    }
  }

  def mergeIfPossible(that: AtalaObjectInfo): Option[AtalaObjectInfo] = {
    if (canAppendAtalaObject(that)) {
      appendAtalaObject(that)
    } else {
      None
    }
  }

  private def canAppendAtalaObject(that: AtalaObjectInfo): Boolean = {
    val sizeMaybe = for {
      thisSize <- estimateTxMetadataSize
      thatSize <- that.estimateTxMetadataSize
    } yield (thisSize + thatSize < TX_METADATA_MAX_SIZE) && (!this.processed) && (!that.processed)

    val versionsMatch = for {
      thisBlock <- getAtalaBlock
      thatBlock <- that.getAtalaBlock
    } yield thisBlock.version == thatBlock.version

    sizeMaybe.getOrElse(false) & versionsMatch.getOrElse(false)
  }

  lazy val estimateTxMetadataSize: Option[Int] = {
    getAndValidateAtalaObject
      .map(AtalaObjectMetadata.estimateTxMetadataSize)
  }

  override def equals(obj: Any): Boolean =
    obj match {
      case AtalaObjectInfo(thatObjectId, thatByteContent, thatProcessed, thatTransaction) =>
        val thatTuple = (thatObjectId, thatByteContent.toList, thatProcessed, thatTransaction)
        val thisTuple = (objectId, byteContent.toList, processed, transaction)
        thisTuple.equals(thatTuple)
      case _ =>
        false
    }

  override def hashCode(): Int =
    (objectId, byteContent.toList, processed, transaction).hashCode()
}
