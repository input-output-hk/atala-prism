package io.iohk
import org.scalactic.Or

package object cef {
  type TableId = String

  type ContainerId = LedgerId Or TableId
  type LedgerId = String
}
