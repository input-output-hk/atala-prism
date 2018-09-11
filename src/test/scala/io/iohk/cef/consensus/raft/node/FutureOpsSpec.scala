package io.iohk.cef.consensus.raft.node

import io.iohk.cef.consensus.raft.node.FutureOps.sequenceForgiving
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.failed

class FutureOpsSpec extends FlatSpec {

  "sequenceForgiving" should "be equivalent to Future.sequence for successful futures" in {
    val futures: Seq[Future[Int]] = Seq(Future[Int](1), Future[Int](2))

    val futureSequence = Future.sequence(futures).futureValue
    val forgivingSequence = sequenceForgiving(futures).futureValue

    futureSequence shouldBe forgivingSequence
  }

  it should "remove failed futures from the result sequence without failing" in {
    val futures: Seq[Future[Int]] = Seq(Future[Int](1), failed[Int](new Exception()))

    val forgivingSequence = sequenceForgiving(futures).futureValue

    forgivingSequence shouldBe Seq(1)
  }
}
