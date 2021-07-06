package io.iohk.atala.cvp.webextension.popup.models

import org.scalajs.dom.raw.File

case class ImageLogo(file: File, width: Int, height: Int) {
  val maxHeight = 50
  val maxWidth = 50
  val hasValidDimensions: Boolean = width <= maxWidth && height <= maxHeight
}
