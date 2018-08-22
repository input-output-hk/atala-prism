package io.iohk.cef
import io.iohk.cef.network.NodeInfo

package object core {
  type DestinationDescriptor = (NodeInfo => Boolean)
  type LedgerId = Int
}
