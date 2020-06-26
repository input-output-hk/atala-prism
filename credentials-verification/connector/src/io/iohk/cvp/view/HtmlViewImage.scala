package io.iohk.cvp.view

import java.nio.charset.StandardCharsets
import java.util.Base64

object HtmlViewImage {

  /**
    * Returns the data URI of an image to be put in {@code <img src="..." />}
    */
  def imageSource(image: String): String = {
    require(image.endsWith(".svg"), "Only SVG images are supported")

    val encodedBytes = Base64.getEncoder.encode(readImage(image))
    val encodedString = new String(encodedBytes, StandardCharsets.UTF_8)
    s"data:image/svg+xml;base64,$encodedString"
  }

  private def readImage(image: String): Array[Byte] = {
    try {
      scala.io.Source.fromResource(s"cvp/view/images/$image").map(_.toByte).toArray
    } catch {
      case _: Throwable => throw new RuntimeException(s"Image $image not found")
    }
  }
}
