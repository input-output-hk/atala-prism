package io.iohk.atala.prism.view

import java.nio.charset.StandardCharsets
import java.util.Base64

object HtmlViewImage {

  /**
    * Returns the image encoded in base64 as a UTF-8 string.
    */
  def imageBase64(image: String): String = {
    val encodedBytes = Base64.getEncoder.encode(readImage(image))
    new String(encodedBytes, StandardCharsets.UTF_8)
  }

  /**
    * Returns the data URI of an image to be put in {@code <img src="..." />}
    */
  def imageSource(image: String): String = {
    require(image.endsWith(".svg"), "Only SVG images are supported")
    s"data:image/svg+xml;base64,${imageBase64(image)}"
  }

  private def readImage(image: String): Array[Byte] = {
    try {
      scala.io.Source.fromResource(s"cvp/view/images/$image").map(_.toByte).toArray
    } catch {
      case _: Throwable => throw new RuntimeException(s"Image $image not found")
    }
  }
}
