package io.iohk.cef.test
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError

case class TestDataItemError(something: Int) extends DataItemError

class InvalidValidation[T](something: Int) extends CanValidate[T] {
  override def validate(t: T): Either[ApplicationError, Unit] = Right(TestDataItemError(something))
}

class ValidValidation[T] extends CanValidate[T] {
  override def validate(t: T): Either[ApplicationError, Unit] = Right(())
}
