package io.iohk.cef.ledger.identity.storage

import akka.util.ByteString
import io.iohk.cef.ledger.identity.IdentityLedgerState
import io.iohk.cef.ledger.identity.storage.db.{IdentityLedgerStateTable, LedgerStateEntryMap}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class LedgerStateStorageImpl  extends LedgerStateStorage[Future, String, Set[ByteString]] {

  DBs.setup('default)

  override def slice(keys: Set[String]): IdentityLedgerState = {
    val db = createDb
    readOnly(db) { implicit session =>
      executeSlice(keys)
    }
  }

  def slice(db: DB)(keys: Set[String]): IdentityLedgerState = {
    inTx(db) { implicit session =>
      executeSlice(keys)
    }
  }

  private def executeSlice(keys: Set[String])(implicit session: DBSession) = {
    val st = IdentityLedgerStateTable.syntax("st")
    val pairs =
      sql"""
      select ${st.result.*} from ${IdentityLedgerStateTable as st}
       where ${st.identity} in (${keys})
      """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply()
    val emptyEntries = LedgerStateEntryMap[String, ByteString]()
    val aggregatedEntries =
      pairs.foldLeft(emptyEntries)(_ aggregateWith _)
    IdentityLedgerState(aggregatedEntries.map)
  }

  override def update(previousState: IdentityLedgerState, newState: IdentityLedgerState): Future[Unit] = {
    begin(db => {
      update(db)(previousState, newState)
    })
  }

  def update(db: DB)(previousState: IdentityLedgerState, newState: IdentityLedgerState): Future[Unit] = {
    val currentState = slice(db)(previousState.keys)
    if (previousState != currentState)
      Future.failed(new IllegalArgumentException("Provided previous state must be equal to the current state"))
    else {
      inTx(db) { implicit session =>
        val keysToAdd = (newState.keys diff currentState.keys)
        val keysToRemove = (currentState.keys diff newState.keys)
        for {
          _ <- Future.sequence(keysToAdd.map(key => newState.get(key).getOrElse(Set()).map(value =>
            insert(db)(key, value)
          )).flatten)
          _ <- Future.sequence(keysToRemove.map(key => currentState.get(key).getOrElse(Set()).map(value =>
            remove(db)(key, value)
          )).flatten)
          _ <- Future.sequence((newState.keys intersect currentState.keys).map { key => {
            val values = newState.get(key).getOrElse(Set())
            val valuesToAdd = (values diff currentState.get(key).getOrElse(Set()))
            val valuesToRemove = (currentState.get(key).getOrElse(Set()) diff values)
            for {
              _ <- Future.sequence(valuesToAdd.map(v => insert(db)(key, v)))
              _ <- Future.sequence(valuesToRemove.map(v => remove(db)(key, v)))
            } yield ()
          }
          })
        } yield ()
      }
    }
  }

  def insert(db: DB)(identity: String, publicKey: ByteString) = {
    val column = IdentityLedgerStateTable.column
    inTx(db) { implicit session =>
      Future {
        sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${publicKey.toArray})
            """.executeUpdate.apply()
      }
    }
  }

  def remove(db: DB)(identity: String, publicKey: ByteString) = {
    val column = IdentityLedgerStateTable.column
    inTx(db) { implicit session =>
      Future {
        sql"""
            delete from ${IdentityLedgerStateTable.table}
             where ${column.identity} = ${identity} and ${column.publicKey} = ${Hex.toHexString(publicKey.toArray)}
            """.executeUpdate.apply()
      }
    }
  }

  protected def begin[T](f: DB => Future[T]): Future[T] = {
    val theDb = createDb
    val tx = theDb.newTx
    tx.begin()
    val result = f(theDb)
    result andThen {
      case Success(_) =>
        theDb.commit()
        theDb.close()
      case Failure(_) =>
        theDb.rollbackIfActive()
        theDb.close()
    }
  }

  protected def createDb: DB = {
    val conn = ConnectionPool.borrow()
    DB(conn)
  }

  /**
    * Wraps the block into a db transaction. Method created for the purpose of testing
    * @param block
    * @tparam T
    * @return
    */
  protected def inTx[T](db: DB)(block: DBSession => T) = {
    db withinTx {
      block
    }
  }

  protected def readOnly[T](db: DB)(block: DBSession => T) = {
    db readOnly {
      block
    }
  }
}
