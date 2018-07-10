package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityLedger.LedgerStateImpl
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.storage.db.{IdentityLedgerStateTable, LedgerStateAggregatedEntries}
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class StateStorage  extends LedgerStateStorage[Future, IdentityLedgerState, String] {

  DBs.setup('default)

  override def slice(keys: Set[String]): IdentityLedgerState = {
    begin(db => {
      slice(db)(keys)
      //TODO: eliminate get
    }).get
  }

  def slice(db: DB)(keys: Set[String]): IdentityLedgerState = {
    val st = IdentityLedgerStateTable.syntax("st")
    db.readOnly { implicit session =>
      val pairs =
        sql"""
      select ${st.identity}, ${st.publicKey} from ${IdentityLedgerStateTable as st}
       where ${st.identity} in (${keys})
      """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply()
      val emptyEntries = LedgerStateAggregatedEntries[String, ByteString]()
      val aggregatedEntries =
        pairs.foldLeft(emptyEntries)(_.aggregate(_))
      new LedgerStateImpl(aggregatedEntries.map)
    }
  }

  override def update[B <: Block[IdentityLedgerState, String]](previousHash: ByteString, newState: IdentityLedgerState): Future[Unit] = {
    begin(db => {
      update(db)(previousHash, newState)
      //TODO: eliminate get
    }).get
  }

  def update[B <: Block[IdentityLedgerState, String]](db: DB)(previousHash: ByteString, newState: IdentityLedgerState): Future[Unit] = {
    val result = begin(db => {
      val currentState = slice(db)(newState.keys)
      inTx(db) { implicit session =>
        Future.sequence(newState.iterator.map {
          case (key, values) =>
            for {
              _ <- Future.sequence((values diff currentState.get(key).getOrElse(Set())).map(v => insert(db)(key, v)))
              _ <- Future.sequence((currentState.get(key).getOrElse(Set()) diff values).map(v => remove(db)(key, v)))
            } yield ()
            //TODO: Need to handle the case when a key is removed (i.e. compare keys after comparing values)
        }).map (_ => ())
      }
    })
    Future.fromTry(result).flatten
  }

  def insert(db: DB)(identity: String, publicKey: ByteString) = {
    val column = IdentityLedgerStateTable.column
    inTx(db) { implicit session =>
      Future {
        sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${publicKey})
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
             where ${column.identity} = ${identity} and ${column.publicKey} = ${publicKey}
            """.executeUpdate.apply()
      }
    }
  }


  def begin[T](f: DB => T): Try[T] = {
    val theDb = DB(ConnectionPool.borrow())
    val tx = theDb.newTx
    tx.begin()
    val result = Try(f(theDb))
    result match {
      case Success(_) => theDb.commit()
      case Failure(_) => theDb.rollbackIfActive()
    }
    theDb.close()
    result
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
}
