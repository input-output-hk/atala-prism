//package io.iohk.cef.ledger.storage
//
//import akka.util.ByteString
//import io.iohk.cef.ledger.identity.IdentityLedgerState
//import io.iohk.cef.ledger.storage.db.IdentityLedgerStateTable
//import scalikejdbc._
//import scalikejdbc.config.DBs
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//sealed trait PersistentIdentityLedgerState extends IdentityLedgerState[String, ByteString] {
//  type Identity = String
//  type PublicKey = ByteString
//
//  def get(identity: Identity): Option[Set[PublicKey]]
//  def put(identity: Identity, publicKey: PublicKey): Future[Unit]
//  def remove(identity: Identity, publicKey: PublicKey): Future[Unit]
//  def containsIdentity(identity: Identity): Future[Boolean]
//  def containsPair(identity: Identity, publicKey: PublicKey): Future[Boolean]
//
//  def isInTx(): Boolean
//
////  def withinDbTransaction(block: DB => Unit): Unit = {
////    val db = DB(ConnectionPool.borrow())
////    try {
////      db.begin()
////      block(db)
////      db.commit()
////    } finally { db.close() }
////  }
//}
//
//case class PersistentIdentityLedgerStateImpl(db: Option[DB]) extends PersistentIdentityLedgerState {
//
//  DBs.setup('default)
//
//  override def begin(): Unit = {
//    if(isInTx()) throw new IllegalStateException("Cannot begin when inTx is true")
//    else {
//      val theDb = DB(ConnectionPool.borrow())
//      theDb.begin()
//      copy(Some(theDb))
//    }
//  }
//
//  override def isInTx(): Boolean = db.isDefined
//
//  override def commit(): this.type = {
//    if(!isInTx()) throw new IllegalStateException("Cannot commit when inTx is false")
//    else {
//      db.get.commit()
//      db.get.close()
//      new PersistentIdentityLedgerStateImpl(None)
//    }
//  }
//
//  override def rollback(): this.type = {
//    if(!isInTx()) throw new IllegalStateException("Cannot rollback when inTx is false")
//    else {
//      db.get.rollbackIfActive()
//      db.get.close()
//      new PersistentIdentityLedgerStateImpl(None)
//    }
//  }
//
//  override def containsIdentity(identity: String): Future[Boolean] = {
//    if(isInTx()) {
//      val st = IdentityLedgerStateTable.syntax("st")
//      inTx(db.get) { implicit session =>
//        Future {
//          sql"""
//          select ${st.identity}, ${st.publicKey} from ${IdentityLedgerStateTable as st}
//           where ${st.identity} = ${identity}
//          """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply().size > 0
//        }
//      }
//    } else Future.failed(new IllegalStateException("Tx is not initialized"))
//  }
//
//  override def containsPair(identity: String, publicKey: ByteString): Future[Boolean] = {
//    if(isInTx()) {
//      val st = IdentityLedgerStateTable.syntax("st")
//      inTx(db.get) { implicit session =>
//        Future {
//          sql"""
//          select ${st.identity}, ${st.publicKey} from ${IdentityLedgerStateTable as st}
//           where ${st.identity} = ${identity} and ${st.publicKey} = ${publicKey}
//          """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).toOption().apply().isDefined
//        }
//      }
//    } else Future.failed(new IllegalStateException("Tx is not initialized"))
//  }
//
//  override def get(identity: String): Future[Option[Set[ByteString]]] = {
//    if(isInTx()) {
//      val st = IdentityLedgerStateTable.syntax("st")
//      inTx(db.get) { implicit session =>
//        Future {
//          val list =
//            sql"""
//          select ${st.identity}, ${st.publicKey} from ${IdentityLedgerStateTable as st}
//           where ${st.identity} = ${identity}
//          """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply()
//          if (list.isEmpty) None
//          else Some(
//            list.tail.foldLeft(list.head.toAggregatedEntry)((s, c) => s.aggregate(c)).keys
//          )
//        }
//      }
//    } else Future.failed(new IllegalStateException("Tx is not initialized"))
//  }
//
//  override def put(identity: String, publicKey: ByteString): Future[Unit] = {
//    if(isInTx()) {
//      Future {
//        for {
//          containsPair <- containsPair(identity, publicKey)
//        } yield {
//          if (!containsPair) {
//            val column = IdentityLedgerStateTable.column
//            inTx(db.get) { implicit session =>
//              sql"""
//            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
//              values (${identity}, ${publicKey})
//            """.executeUpdate.apply()
//            }
//          }
//        }
//      }
//    } else Future.failed(new IllegalStateException("Tx is not initialized"))
//  }
//
//  override def remove(identity: String, publicKey: ByteString): Future[Unit] = {
//    if(isInTx()) {
//      Future {
//        for {
//          containsPair <- containsPair(identity, publicKey)
//        } yield {
//          if (containsPair) {
//            val column = IdentityLedgerStateTable.column
//            inTx(db.get) { implicit session =>
//              sql"""
//            delete from ${IdentityLedgerStateTable.table}
//             where ${column.identity} = ${identity} and ${column.publicKey} = ${publicKey}
//            """.executeUpdate.apply()
//            }
//          }
//        }
//      }
//    } else Future.failed(new IllegalStateException("Tx is not initialized"))
//  }
//
//  /**
//    * Wraps the block into a db transaction. Method created for the purpose of testing
//    * @param block
//    * @tparam T
//    * @return
//    */
//  protected def inTx[T](db: DB)(block: DBSession => T) = {
//    db withinTx {
//      block
//    }
//  }
//}
