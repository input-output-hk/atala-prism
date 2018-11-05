package io.iohk.cef.frontend.models

import io.iohk.cef.core._

// TODO: consider merging with Envelope type
case class DataItemEnvelope[A](content: A, destinationDescriptor: DestinationDescriptor)
//case class DataItemEnvelope[B, A <: DataItem[B]](content: A, destinationDescriptor: DestinationDescriptor)
