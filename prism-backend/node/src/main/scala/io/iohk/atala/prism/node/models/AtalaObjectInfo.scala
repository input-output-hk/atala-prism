package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.cardano.TX_METADATA_MAX_SIZE
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata
import io.iohk.atala.prism.node.operations.{Operation, parseOperationsFromByteContent}
import io.iohk.atala.prism.protos.node_internal

case class AtalaObjectInfo(
    objectId: AtalaObjectId,
    byteContent: Array[
      Byte
    ], // Serialization of a io.iohk.atala.prism.protos.node_internal.AtalaObject
    operations: List[Operation], // List of parsed operations
    status: AtalaObjectStatus, // Status of an object may be processed (e.g. DIDs were recognized and stored in DB), merged (e.g. merged with another object) or pending
    transaction: Option[
      TransactionInfo
    ] // Blockchain transaction the object was first found in
) {
  def this(
      objectId: AtalaObjectId,
      byteContent: Array[Byte],
      status: AtalaObjectStatus,
      transaction: Option[TransactionInfo] = None
  ) =
    this(
      objectId,
      byteContent,
      parseOperationsFromByteContent(byteContent),
      status,
      transaction
    )

  lazy val getAndValidateAtalaObject: Option[node_internal.AtalaObject] =
    node_internal.AtalaObject.validate(byteContent).toOption

  lazy val getAtalaBlock: Option[node_internal.AtalaBlock] = {
    for {
      atalaObject <- getAndValidateAtalaObject
      atalaBlock <- atalaObject.blockContent
    } yield atalaBlock
  }

  private def appendAtalaObject(
      that: AtalaObjectInfo
  ): Option[AtalaObjectInfo] = {
    for {
      thisBlock <- getAtalaBlock
      thatBlock <- that.getAtalaBlock
    } yield {
      val mergedBlock =
        node_internal.AtalaBlock(thisBlock.operations ++ thatBlock.operations)
      val obj = node_internal
        .AtalaObject()
        .withBlockContent(mergedBlock)
        .withBlockOperationCount(mergedBlock.operations.size)
        .withBlockByteLength(mergedBlock.toByteArray.length)
      AtalaObjectInfo(
        AtalaObjectId.of(obj),
        obj.toByteArray,
        operations ++ that.operations,
        status = AtalaObjectStatus.Pending,
        None
      )
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
    } yield (thisSize + thatSize < TX_METADATA_MAX_SIZE) && (this.status == AtalaObjectStatus.Pending) && (that.status == AtalaObjectStatus.Pending)

    sizeMaybe.getOrElse(false)
  }

  lazy val estimateTxMetadataSize: Option[Int] = {
    getAndValidateAtalaObject
      .map(AtalaObjectMetadata.estimateTxMetadataSize)
  }

  override def equals(obj: Any): Boolean =
    obj match {
      case AtalaObjectInfo(
            thatObjectId,
            thatByteContent,
            _,
            thatStatus,
            thatTransaction
          ) =>
        val thatTuple =
          (thatObjectId, thatByteContent.toList, thatStatus, thatTransaction)
        val thisTuple = (objectId, byteContent.toList, status, transaction)
        thisTuple.equals(thatTuple)
      case _ =>
        false
    }

  override def hashCode(): Int =
    (objectId, byteContent.toList, status, transaction).hashCode()
}

object AtalaObjectInfo {
  def apply(
      objectId: AtalaObjectId,
      byteContent: Array[Byte],
      status: AtalaObjectStatus,
      transaction: Option[TransactionInfo] = None
  ) = new AtalaObjectInfo(objectId, byteContent, status, transaction)
}
