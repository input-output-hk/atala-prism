package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.operations.CreateDIDOperationSpec
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.node_internal

class AtalaObjectInfoSpec extends AtalaWithPostgresSpec {

  private val dummyAtalaOperation = BlockProcessingServiceSpec.createDidOperation
  private val dummySignedOperations = (0 to 3).indices.map { masterId =>
    BlockProcessingServiceSpec.signOperation(
      dummyAtalaOperation,
      s"master$masterId",
      CreateDIDOperationSpec.masterKeys.privateKey
    )
  }

  private def createAtalaObject(ops: Seq[SignedAtalaOperation], processed: Boolean = false) = {
    val blockContent = node_internal.AtalaObject(
      blockOperationCount = ops.size,
      block = node_internal.AtalaObject.Block.BlockContent(
        node_internal.AtalaBlock(version = "1.0", operations = ops)
      )
    )
    AtalaObjectInfo(
      objectId = AtalaObjectId.of(blockContent),
      blockContent.toByteArray,
      processed = processed
    )
  }

  "AtalaObject.mergeIfPossible" should {
    "merge two valid objects" in {
      val atalaObject1 = createAtalaObject(dummySignedOperations.take(2))
      val atalaObject2 = createAtalaObject(dummySignedOperations.drop(2))
      val atalaObjectMerged = atalaObject1.mergeIfPossible(atalaObject2).get
      val expectedAtalaObject = createAtalaObject(dummySignedOperations)

      atalaObjectMerged must be(expectedAtalaObject)
    }

    "not merge objects if metadata size exceeds TX_METADATA_MAX_SIZE" in {
      val atalaOperations = (0 to 20).toList.map { masterId =>
        BlockProcessingServiceSpec.signOperation(
          BlockProcessingServiceSpec.createDidOperation,
          s"master$masterId",
          CreateDIDOperationSpec.masterKeys.privateKey
        )
      }
      val atalaObject1 = createAtalaObject(atalaOperations.take(10))
      val atalaObject2 = createAtalaObject(atalaOperations.drop(10))
      val maybeMerged = atalaObject1.mergeIfPossible(atalaObject2)

      maybeMerged must be(None)
    }

    "not merge objects if one of them was processed" in {
      val processedAtalaObject = createAtalaObject(dummySignedOperations.take(2), processed = true)
      val atalaObject = createAtalaObject(dummySignedOperations.drop(2))
      val maybeMerged1 = processedAtalaObject.mergeIfPossible(atalaObject)
      val maybeMerged2 = atalaObject.mergeIfPossible(processedAtalaObject)

      maybeMerged1 must be(None)
      maybeMerged2 must be(None)
    }

    "not merge objects if one of them is invalid" in {
      val invalidBlockContent = "invalidBlockContent".getBytes()
      val invalidAtalaObject = AtalaObjectInfo(
        objectId = AtalaObjectId.of(invalidBlockContent),
        invalidBlockContent,
        processed = false
      )
      val atalaObject = createAtalaObject(dummySignedOperations)
      val maybeMerged1 = atalaObject.mergeIfPossible(invalidAtalaObject)
      val maybeMerged2 = invalidAtalaObject.mergeIfPossible(atalaObject)

      maybeMerged1 must be(None)
      maybeMerged2 must be(None)
    }
  }
}
