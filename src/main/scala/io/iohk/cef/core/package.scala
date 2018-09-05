package io.iohk.cef
import io.iohk.cef.network.NodeId

package object core {
  type DestinationDescriptor = (NodeId => Boolean)
}
