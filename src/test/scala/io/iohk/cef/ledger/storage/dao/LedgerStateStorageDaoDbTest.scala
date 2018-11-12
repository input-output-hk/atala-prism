package io.iohk.cef.ledger.storage.dao
import akka.util.ByteString
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.scalike.LedgerStateTable
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.LedgerState
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio._

trait LedgerStateStorageDaoDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers with MockitoSugar {

  private def insertPairs(ledgerId: LedgerId, pairs: Seq[(String, ByteString)])(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    pairs.foreach {
      case (key, serializedValue) =>
        sql"""insert into ${LedgerStateTable.table} (${column.ledgerStateId}, ${column.partitionId}, ${column.data})
            values (${ledgerId}, ${key}, ${serializedValue.toArray})
         """.update().apply()
    }
  }

  behavior of "LedgerStateStorageDao"

  it should "retrieve an slice" in { implicit session =>
    val byteStringSerializable = mock[NioEncDec[Set[ByteString]]]
    val identityName = "carlos"
    val otherIdentity = "otherIdentity"
    val publicKey1 = ByteString("pp1")
    val publicKey2 = ByteString("pp2")
    val publicKeys = Set(publicKey1, publicKey2)
    val serializedKeys = ByteString("serializedKeys")
    val otherSerializedKeys = ByteString("serializedKeys2")
    val otherPublicKeys = Set(publicKey1)
    when(byteStringSerializable.encode(publicKeys)).thenReturn(serializedKeys.toByteBuffer)
    when(byteStringSerializable.decode(serializedKeys.toByteBuffer)).thenReturn(Some(publicKeys))
    insertPairs("1", Seq(identityName -> serializedKeys, otherIdentity -> otherSerializedKeys))
    val dao = new LedgerStateStorageDao[Set[ByteString]]()
    implicit val bsSerializable = byteStringSerializable
    dao.slice("1", Set(identityName)) mustBe LedgerState(Map(identityName -> publicKeys))
    dao.slice("1", Set(identityName, "unexistent")) mustBe LedgerState(Map(identityName -> publicKeys))
    when(byteStringSerializable.encode(otherPublicKeys)).thenReturn(otherSerializedKeys.toByteBuffer)
    when(byteStringSerializable.decode(otherSerializedKeys.toByteBuffer)).thenReturn(Some(otherPublicKeys))
    dao.slice("1", Set(identityName, otherIdentity)) mustBe
      LedgerState(Map(identityName -> publicKeys, otherIdentity -> otherPublicKeys))
  }

  it should "update a state" in { implicit session =>
    val byteStringSerializable = mock[NioEncDec[Set[ByteString]]]
    val identityName = "carlos"
    val otherIdentity = "otherIdentity"
    val publicKey1 = ByteString("pp1")
    val publicKey2 = ByteString("pp2")
    val publicKeys = Set(publicKey1, publicKey2)
    val serializedKeys = ByteString("serializedKeys")
    val serializedPublicKey1Set = ByteString("serializedKeys2")
    val publicKey1Set = Set(publicKey1)
    when(byteStringSerializable.encode(publicKeys)).thenReturn(serializedKeys.toByteBuffer)
    when(byteStringSerializable.decode(serializedKeys.toByteBuffer)).thenReturn(Some(publicKeys))
    when(byteStringSerializable.encode(publicKey1Set)).thenReturn(serializedPublicKey1Set.toByteBuffer)
    when(byteStringSerializable.decode(serializedPublicKey1Set.toByteBuffer)).thenReturn(Some(publicKey1Set))
    val dao = new LedgerStateStorageDao[Set[ByteString]]()
    implicit val bsSerializable = byteStringSerializable
    val emptyLs = LedgerState[Set[ByteString]](Map())
    dao.slice("1", Set(identityName)) mustBe emptyLs
    val firstLs = LedgerState(Map(identityName -> publicKey1Set))
    dao.update("1", emptyLs, firstLs)

    dao.slice("1", Set(identityName)) mustBe LedgerState(Map(identityName -> publicKey1Set))
    val secondLs = LedgerState(Map(identityName -> publicKeys))
    dao.update("1", firstLs, secondLs)

    dao.slice("1", Set(identityName)) mustBe LedgerState(Map(identityName -> publicKeys))
    val thirdLs = LedgerState(Map(identityName -> publicKey1Set, otherIdentity -> publicKey1Set))
    dao.update("1", secondLs, thirdLs)

    dao.slice("1", Set(identityName, otherIdentity)) mustBe
      LedgerState(Map(identityName -> publicKey1Set, otherIdentity -> publicKey1Set))
    val fourthLs = LedgerState(Map(identityName -> publicKey1Set))
    dao.update("1", thirdLs, fourthLs)

    dao.slice("1", Set(identityName, otherIdentity)) mustBe
      LedgerState(Map(identityName -> publicKey1Set))
    intercept[IllegalArgumentException] {
      dao.update("1", secondLs, thirdLs)
    }
  }
}
