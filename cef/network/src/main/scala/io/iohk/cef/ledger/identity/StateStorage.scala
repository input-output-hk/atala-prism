package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityLedger.LedgerStateImpl
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.identity.db.{IdentityLedgerStateTable, LedgerStateAggregatedEntries}
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._
import scalikejdbc.config._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class StateStorage  extends LedgerStateStorage[Future, IdentityLedgerState, String] {

  DBs.setup('default)

  override def slice(keys: Set[String]): IdentityLedgerState = {
    beginTry(db => {
      slice(db)(keys)
    })
  }

  def slice(db: DB)(keys: Set[String]): IdentityLedgerState = {
    val st = IdentityLedgerStateTable.syntax("st")
    inTx(db) { implicit session =>
      val pairs =
        sql"""
      select ${st.result.*} from ${IdentityLedgerStateTable as st}
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
    })
  }

  def update[B <: Block[IdentityLedgerState, String]](db: DB)(previousHash: ByteString, newState: IdentityLedgerState): Future[Unit] = {
    begin(db => {
      val currentState = slice(db)(newState.keys)
      inTx(db) { implicit session =>
        val keysToAdd = (newState.keys diff currentState.keys)
        val keysToRemove = (currentState.keys diff newState.keys)
        println()
        for {
          _ <- Future.sequence(keysToAdd.map(key => newState.get(key).getOrElse(Set()).map(value =>
            insert(db)(key, value)
          )).flatten)
          _ <- Future.sequence(keysToRemove.map(key => newState.get(key).getOrElse(Set()).map(value =>
            remove(db)(key, value)
          )).flatten)
          _ <- Future.sequence((newState.keys intersect currentState.keys).map { key => {
                  val values = newState.get(key).getOrElse(Set())
                  for {
                    _ <- Future.sequence((values diff currentState.get(key).getOrElse(Set())).map(v =>
                      insert(db)(key, v)
                    ))
                    _ <- Future.sequence((currentState.get(key).getOrElse(Set()) diff values).map(v =>
                      remove(db)(key, v)
                    ))
                  } yield ()
                }
              })
        } yield ()
      }
    })
  }

  def insert(db: DB)(identity: String, publicKey: ByteString) = {
    val column = IdentityLedgerStateTable.column
    inTx(db) { implicit session =>
      Future {
        sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${Hex.toHexString(publicKey.toArray)})
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


  def begin[T](f: DB => Future[T]): Future[T] = {
    val theDb = DB(ConnectionPool.borrow())
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

  def beginTry[T](f: DB => T): T = {
    val conn = ConnectionPool.borrow()
    val theDb = DB(conn)
    val tx = theDb.newTx
    tx.begin()
    val result = Try(f(theDb))
    result match {
      case Success(_) =>
        theDb.commit()
        theDb.close()
      case Failure(_) =>
        theDb.rollbackIfActive()
        theDb.close()
    }
    //TODO: bubble up the Try
    result.get
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
