package io.iohk.atala.prism.interop
import io.iohk.atala.prism.protos.node_models
import pbandk.MessageKt

object toScalaProtos {

  implicit class DIDDataInterop(private val v: io.iohk.atala.prism.protos.DIDData) extends AnyVal {
    def asScala: node_models.DIDData = {
      node_models.DIDData.parseFrom(MessageKt.encodeToByteArray(v))
    }
  }

  implicit class AtalaOperationInterop(private val v: io.iohk.atala.prism.protos.AtalaOperation) extends AnyVal {
    def asScala: node_models.AtalaOperation =
      node_models.AtalaOperation.parseFrom(MessageKt.encodeToByteArray(v))
  }
}
