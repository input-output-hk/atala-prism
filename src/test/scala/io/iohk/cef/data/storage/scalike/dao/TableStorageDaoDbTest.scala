package io.iohk.cef.data.storage.scalike.dao

import io.iohk.cef.crypto._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data.{DataItem, DataItemId, Owner, Witness}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.codecs.nio._

import io.iohk.cef.test.{InvalidValidation, ValidValidation}

trait TableStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with MockitoSugar
    with EitherValues {

  behavior of "TableStorageDaoDbTest"

  it should "insert data items" in { implicit s =>
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val validValidation = new ValidValidation[String]
    val invalidValidation = new InvalidValidation[String](10)
    val dataItems = Seq(
      DataItem("valid", "data", Seq(), Seq(Owner(ownerKeyPair.public))),
      DataItem("valid2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public))))
    val dao = new TableStorageDao

    val itemsBefore = selectAll[String](dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(_))

    val itemsAfter = selectAll[String](dataItems.map(_.id))
    itemsAfter.isRight mustBe true
    itemsAfter.right.value.sortBy(_.id) mustBe dataItems.sortBy(_.id)
  }

  it should "delete data items" in { implicit s =>
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(
      DataItem("valid", "data", Seq(), Seq(Owner(ownerKeyPair.public))),
      DataItem("valid2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public))))
    val dao = new TableStorageDao

    val itemsBefore = selectAll[String](dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(_))
    val itemsMiddle = selectAll[String](dataItems.map(_.id))
    itemsMiddle.isRight mustBe true
    itemsMiddle.right.value.sortBy(_.id) mustBe dataItems.sortBy(_.id)
    dataItems.foreach(item => dao.delete(item.id))
    val itemsAfter = selectAll[String](dataItems.map(_.id))
    itemsAfter mustBe Right(Seq())
  }

  private def selectAll[I](ids: Seq[DataItemId])(
      implicit session: DBSession,
      serializable: NioEncDec[I]): Either[CodecError, Seq[DataItem[I]]] = {
    import io.iohk.cef.utils.EitherTransforms
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.dataItemId} in (${ids})
       """.map(rs => DataItemTable(di.resultName)(rs)).list().apply()
    //Inefficient for large tables
    import EitherTransforms._
    val dataItems = dataItemRows.map(dir => {
      val ownerEither = selectDataItemOwners(dir.dataItemId).toEitherList
      val witnessEither = selectDataItemWitnesses(dir.dataItemId).toEitherList
      for {
        owners <- ownerEither
        witnesses <- witnessEither
      } yield
        DataItem(
          dir.dataItemId,
          serializable
            .decode(dir.dataItem.toByteBuffer)
            .getOrElse(throw new IllegalStateException(s"Could not decode data item: ${dir}")),
          witnesses,
          owners
        )
    })
    dataItems.toEitherList
  }

  private def selectDataItemWitnesses(dataItemId: DataItemId)(implicit session: DBSession) = {
    val dis = DataItemSignatureTable.syntax("dis")
    sql"""
        select ${dis.result.*}
        from ${DataItemSignatureTable as dis}
        where ${dis.dataItemId} = ${dataItemId}
       """
      .map { rs =>
        val row = DataItemSignatureTable(dis.resultName)(rs)
        for {
          key <- SigningPublicKey.decodeFrom(row.signingPublicKey)
          sig <- Signature.decodeFrom(row.signature)
        } yield Witness(key, sig)
      }
      .list()
      .apply()
  }

  private def selectDataItemOwners(dataItemId: DataItemId)(implicit session: DBSession) = {
    val dio = DataItemOwnerTable.syntax("dio")
    sql"""
        select ${dio.result.*}
        from ${DataItemOwnerTable as dio}
        where ${dio.dataItemId} = ${dataItemId}
       """
      .map(rs => SigningPublicKey.decodeFrom(DataItemOwnerTable(dio.resultName)(rs).signingPublicKey).map(Owner))
      .list()
      .apply()
  }

}
