package io.iohk.cef.consensus.raft.node

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful

object FutureOps {
  // A version of Future.sequence that removes failed
  // future results from the result. This allows the
  // sequence operation to succeed when one or more of the
  // input futures fails. In the scenario where all Futures
  // succeed the result of this function is the same as
  // Future.sequence. In the scenario where all Futures fail
  // the result is Future(Seq.empty).
  def sequenceForgiving[A](in: Seq[Future[A]])(implicit ec: ExecutionContext): Future[Seq[A]] = {
    val zero: Future[List[Option[A]]] = successful(List())
    val fld: Future[List[Option[A]]] = in.foldLeft(zero) { (acc: Future[List[Option[A]]], fa: Future[A]) =>
      {
        val eventualMaybeA: Future[Option[A]] = fa.map(a => Some(a)).fallbackTo(Future(None))
        acc.zipWith(eventualMaybeA)((list, v) => v :: list)
      }
    }
    fld.map(l => l.flatten.reverse)
  }
}
