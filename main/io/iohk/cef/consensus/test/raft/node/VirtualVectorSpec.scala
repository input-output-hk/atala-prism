package io.iohk.cef.consensus.raft.node
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.annotation.tailrec

class VirtualVectorSpec extends FlatSpec {

  behavior of "VirtualVector"

  it should "enable virtual indexing" in {
    val v = new VirtualVector[String](Vector("A", "B", "C"), 2, IndexedSeq("D", "E"))

    v.size shouldBe 3
    v.length shouldBe 3
    v(0) shouldBe "A"
    v(1) shouldBe "D"
    v(2) shouldBe "E"
  }

  it should "support deleting all" in {
    val v = new VirtualVector[String](Vector("A", "B", "C"), 3, IndexedSeq())
    v.size shouldBe 0
    v.isEmpty shouldBe true
    v.nonEmpty shouldBe false
  }

  it should "support lastOption" in {
    new VirtualVector[String](Vector("A", "B", "C"), 0, IndexedSeq()).lastOption shouldBe Some("C")
    new VirtualVector[String](Vector("A", "B", "C"), 3, IndexedSeq()).lastOption shouldBe None
    new VirtualVector[String](Vector("A", "B", "C"), 0, IndexedSeq("D")).lastOption shouldBe Some("D")
    new VirtualVector[String](Vector("A", "B", "C"), 1, IndexedSeq("D")).lastOption shouldBe Some("D")
  }

  it should "support map" in {
    new VirtualVector[String](Vector("A", "B", "C"), 1, IndexedSeq("D"))
      .map(s => s) shouldBe new VirtualVector[String](Vector("A", "B", "D"), 0, IndexedSeq.empty)
    new VirtualVector[String](Vector("A", "B", "C"), 1, IndexedSeq("D")).toSeq shouldBe Seq("A", "B", "D")
  }

  it should "support toString" in {
    new VirtualVector[String](Vector("A", "B", "C"), 1, IndexedSeq("D")).toString shouldBe "VirtualVector(A, B, D)"
  }

  it should "not stack overflow in length" in {
    // given
    val base = Vector("A", "B", "C")

    // when
    val vv = loop(100000, new VirtualVector[String](base, 0, IndexedSeq.empty))

    // then
    vv.length shouldBe 3
  }

  @tailrec
  private def loop(n: Int, vv: VirtualVector[String]): VirtualVector[String] = {
    if (n == 0)
      vv
    else
      loop(n - 1, new VirtualVector[String](vv, 0, IndexedSeq.empty))
  }
}
