package io.iohk.atala.prism.view

import com.google.common.io.ByteStreams

import java.nio.charset.StandardCharsets
import java.util.Base64

object HtmlViewImage {
  private val SVG_MIME_TYPE = "image/svg+xml"
  private val PNG_MIME_TYPE = "image/png"

  /** Returns the image encoded in base64 as a UTF-8 string.
    */
  def imageBase64(image: String): String = {
    val encodedBytes = Base64.getEncoder.encode(readImage(image))
    new String(encodedBytes, StandardCharsets.UTF_8)
  }

  /** Returns the data URI of an image to be put in {@code <img src="..." />}
    */
  def imageSource(image: String): String = {
    val mimeType = imageMimeType(image)
    s"data:$mimeType;base64,${imageBase64(image)}"
  }

  /** Returns the MIME type of the given image, to be used for its data URI.
    */
  def imageMimeType(image: String): String = {
    val extension = image.substring(image.lastIndexOf('.') + 1).toLowerCase
    extension match {
      case "svg" => SVG_MIME_TYPE
      case "png" => PNG_MIME_TYPE
      case _ =>
        throw new IllegalArgumentException(
          s"Image extension $extension not supported"
        )
    }
  }

  private def readImage(image: String): Array[Byte] = {
    try {
      ByteStreams.toByteArray(
        getClass.getResourceAsStream(s"/cvp/view/images/$image")
      )
    } catch {
      case e: Throwable =>
        throw new RuntimeException(s"Image $image not found", e)
    }
  }
}
