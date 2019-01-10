package io.iohk.cef.frontend.controllers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.{ApplicationError => PlaysonifyError}
import io.iohk.cef.error.{ApplicationError => CefError}
import io.iohk.cef.frontend.client
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

package object common {

  def fromFutureEither[T](value: client.Response[T], playsonifyError: PlaysonifyError)(
      implicit ec: ExecutionContext
  ): FutureApplicationResult[T] = {

    value.map {
      case Left(_) => Bad(playsonifyError).accumulating
      case Right(result) => Good(result)
    }
  }

  def fromEither[T](value: Either[CefError, T], playsonifyError: PlaysonifyError) = value match {
    case Left(_) => Bad(playsonifyError).accumulating
    case Right(result) => Good(result)
  }
}
