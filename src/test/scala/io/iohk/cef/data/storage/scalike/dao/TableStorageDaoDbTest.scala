package io.iohk.cef.data.storage.scalike.dao
import org.scalatest.{MustMatchers, fixture}
import org.scalatest.mockito.MockitoSugar
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.crypto._
import io.iohk.cef.data.Owner
import io.iohk.cef.test.DummyValidDataItem

trait TableStorageDaoDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers with MockitoSugar {


  behavior of "TableStorageDaoDbTest"

  it should "insert data items" in { implicit s =>
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val dataItems = Seq(DummyValidDataItem("valid", Seq(Owner(ownerKeyPair.public)), Seq()),
      DummyValidDataItem("valid2", Seq(Owner(ownerKeyPair2.public)), Seq()))
    val dao = new TableStorageDao
    dataItems.foreach(dao.insert(_))
  }
}
