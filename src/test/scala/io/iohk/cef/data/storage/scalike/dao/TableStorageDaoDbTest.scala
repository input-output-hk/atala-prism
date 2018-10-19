package io.iohk.cef.data.storage.scalike.dao
import io.iohk.cef.crypto._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data.{DataItem, DataItemId, Owner, Witness}
import io.iohk.cef.test.DummyValidDataItem
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

trait TableStorageDaoDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers with MockitoSugar {



  def selectAll[I <: DataItem](ids: Seq[DataItemId], dataItemCreator: (String, Seq[Owner], Seq[Witness]) => I)(
    implicit session: DBSession): Either[CodecError, Seq[I]] = {
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
      } yield dataItemCreator(dir.dataItemId ,owners, witnesses)
    })
    dataItems.toEitherList
  }

  private def selectDataItemWitnesses[I <: DataItem](dataItemId: DataItemId)(implicit session: DBSession) = {
    val dis = DataItemSignatureTable.syntax("dis")
    sql"""
        select ${dis.result.*}
        from ${DataItemSignatureTable as dis}
        where ${dis.dataItemId} = ${dataItemId}
       """
      .map(rs => {
        val row = DataItemSignatureTable(dis.resultName)(rs)
        for {
          key <- SigningPublicKey.decodeFrom(row.signingPublicKey)
          sig <- Signature.decodeFrom(row.signature)
        } yield Witness(key, sig)
      })
      .list()
      .apply()
  }

  private def selectDataItemOwners[I <: DataItem](dataItemId: DataItemId)(implicit session: DBSession) = {
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

  private def itemsFromDb(itemIds: Seq[String])(implicit session: DBSession) = selectAll(itemIds, (s, o, w) => DummyValidDataItem(s, o, w))


  behavior of "TableStorageDaoDbTest"

  it should "insert data items" in { implicit s =>
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(DummyValidDataItem("valid", Seq(Owner(ownerKeyPair.public)), Seq()),
      DummyValidDataItem("valid2", Seq(Owner(ownerKeyPair2.public)), Seq()))
    val dao = new TableStorageDao

    val itemsBefore = itemsFromDb(dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(_))
    val itemsAfter = itemsFromDb(dataItems.map(_.id))
    itemsAfter.isRight mustBe true
    itemsAfter.right.get.sortBy(_.id) mustBe dataItems.sortBy(_.id)
  }

  it should "delete data items" in { implicit s =>
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(DummyValidDataItem("valid", Seq(Owner(ownerKeyPair.public)), Seq()),
      DummyValidDataItem("valid2", Seq(Owner(ownerKeyPair2.public)), Seq()))
    val dao = new TableStorageDao

    val itemsBefore = itemsFromDb(dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())
    dataItems.foreach(dao.insert(_))
    val itemsMiddle = itemsFromDb(dataItems.map(_.id))
    itemsMiddle.isRight mustBe true
    itemsMiddle.right.get.sortBy(_.id) mustBe dataItems.sortBy(_.id)
    dataItems.foreach(dao.delete(_))
    val itemsAfter = itemsFromDb(dataItems.map(_.id))
    itemsAfter mustBe Right(Seq())
  }
}
