package io.iohk.atala.prism.node.interop

import io.iohk.atala.prism.protos.node_models
import pbandk.MessageKt

object toScalaProtos {

  implicit class CreateDIDOperationInterop(
      private val v: io.iohk.atala.prism.protos.CreateDIDOperation.DIDCreationData
  ) extends AnyVal {
    def asScala: node_models.CreateDIDOperation.DIDCreationData = {
      node_models.CreateDIDOperation.DIDCreationData.parseFrom(
        MessageKt.encodeToByteArray(v)
      )
    }
  }

  implicit class DIDDataInterop(
      private val v: io.iohk.atala.prism.protos.DIDData
  ) extends AnyVal {
    def asScala: node_models.DIDData = {
      node_models.DIDData.parseFrom(MessageKt.encodeToByteArray(v))
    }
  }

  implicit class AtalaOperationInterop(
      private val v: io.iohk.atala.prism.protos.AtalaOperation
  ) extends AnyVal {
    def asScala: node_models.AtalaOperation =
      node_models.AtalaOperation.parseFrom(MessageKt.encodeToByteArray(v))
  }
}
