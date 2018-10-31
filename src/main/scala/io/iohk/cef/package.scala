package io.iohk
import io.iohk.cef.data.TableId
import org.scalactic.Or

package object cef {
  type ContainerId = LedgerId Or TableId
  type LedgerId = String
}
