package io.iohk.cef.frontend.models

import io.iohk.cef.core._
import io.iohk.cef.data.TableId

// TODO: consider merging with Envelope type
case class DataItemEnvelope[A](content: A, tableId: TableId, destinationDescriptor: DestinationDescriptor)
