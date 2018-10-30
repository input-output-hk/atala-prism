package io.iohk.cef.data
import io.iohk.cef.error.ApplicationError

trait CanValidate[T] {

  def validate(t: T): Either[ApplicationError, Unit]
}
