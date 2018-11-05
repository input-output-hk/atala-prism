package io.iohk.cef.frontend.models

import io.iohk.cef.core._
import io.iohk.cef.data.{DataItem, TableId}

// TODO: consider merging with Envelope type
case class DataItemEnvelope[B, A <: DataItem[B]](
    content: A,
    tableId: TableId,
    destinationDescriptor: DestinationDescriptor)
