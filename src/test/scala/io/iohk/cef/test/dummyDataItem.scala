package io.iohk.cef.test

import io.iohk.cef.data.{DataItem, DataItemError, Owner, Witness}

case class TestDataItemError(something: Int) extends DataItemError

case class DummyInvalidDataItem(
    id: String,
    data: String,
    error: TestDataItemError,
    owners: Seq[Owner],
    witnesses: Seq[Witness])
    extends DataItem[String] {

  override def apply(): Either[DataItemError, Unit] = Left(error)
}

case class DummyValidDataItem(id: String, data: String, owners: Seq[Owner], witnesses: Seq[Witness])
    extends DataItem[String] {

  override def apply(): Either[DataItemError, Unit] = Right(())
}
