package io.iohk.atala.cvp.webextension.popup.models

import org.scalajs.dom.raw.File

case class ImageLogo(file: File, width: Int, height: Int) {
  val hasValidDimensions: Boolean = width <= 50 && height <= 50
}
