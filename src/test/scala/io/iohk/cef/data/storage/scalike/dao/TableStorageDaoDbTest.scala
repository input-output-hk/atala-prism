package io.iohk.cef.data.storage.scalike.dao

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.test.{InvalidValidation, ValidValidation}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

trait TableStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with MockitoSugar
    with EitherValues {

  behavior of "TableStorageDaoDbTest"

  it should "insert data items" in { implicit s =>
    val tableId: TableId = "TableId"
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(
      DataItem("valid", "data", Seq(), Seq(Owner(ownerKeyPair.public))),
      DataItem("valid2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public))))
    val dao = new TableStorageDao

    val itemsBefore = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(tableId, _))

    val itemsAfter = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsAfter.isRight mustBe true
    itemsAfter.right.value.sortBy(_.id) mustBe dataItems.sortBy(_.id)
  }

  it should "delete data items" in { implicit s =>
    val tableId: TableId = "TableId"
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(
      DataItem("valid", "data", Seq(), Seq(Owner(ownerKeyPair.public))),
      DataItem("valid2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public))))
    val dao = new TableStorageDao

    val itemsBefore = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(tableId, _))
    val itemsMiddle = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsMiddle.isRight mustBe true
    itemsMiddle.right.value.sortBy(_.id) mustBe dataItems.sortBy(_.id)
    dataItems.foreach(item => dao.delete(tableId, item))
    val itemsAfter = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsAfter mustBe Right(Seq())
  }

}
