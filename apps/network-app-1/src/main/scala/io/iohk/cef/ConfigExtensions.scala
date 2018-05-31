package io.iohk.cef

import java.net.URI

import com.typesafe.config.Config

import scala.util.Try

object ConfigExtensions {

  implicit class Extensions(val underlying: Config) extends AnyVal {
    def getOption[T](extractor: Config => T): Option[T] =
      Try(extractor(underlying)).toOption

    def getURI(path: String): URI =
      new URI(underlying.getString(path))

  }
}