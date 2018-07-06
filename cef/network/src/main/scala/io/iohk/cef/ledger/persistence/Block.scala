package io.iohk.cef.ledger.persistence

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class Block[State <: LedgerState](header: BlockHeader, transactions: List[Transaction[State]])
    extends (State => Future[State]) {

  override def apply(state: State): Future[State] = {
    state.begin()
    transactions.foldLeft[Future[State]](Future.successful(state))((future, tx) => {
      future.flatMap(tx(_))
    }).andThen {
      //Not good, this runs in a different thread and the state object is not thread safe.
      //Need to provide a better implementation of this "transaction" concept
      case Success(_) =>
        state.commit()
      case Failure(_) =>
        state.rollback()
    }
  }
}
