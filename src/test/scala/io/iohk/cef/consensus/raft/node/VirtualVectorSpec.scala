package io.iohk.cef.consensus.raft.node
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class VirtualVectorSpec extends FlatSpec {

  behavior of "VirtualVector"

  it should "enable virtual indexing" in {
    val v = new VirtualVector[String](Vector("A", "B", "C"), 2, Seq("D", "E"))

    v.size shouldBe 3
    v.length shouldBe 3
    v(0) shouldBe "A"
    v(1) shouldBe "D"
    v(2) shouldBe "E"
  }

  it should "support deleting all" in {
    val v = new VirtualVector[String](Vector("A", "B", "C"), 3, Seq())
    v.size shouldBe 0
    v.isEmpty shouldBe true
    v.nonEmpty shouldBe false
  }

  it should "support lastOption" in {
    new VirtualVector[String](Vector("A", "B", "C"), 0, Seq()).lastOption shouldBe Some("C")
    new VirtualVector[String](Vector("A", "B", "C"), 3, Seq()).lastOption shouldBe None
    new VirtualVector[String](Vector("A", "B", "C"), 0, Seq("D")).lastOption shouldBe Some("D")
    new VirtualVector[String](Vector("A", "B", "C"), 1, Seq("D")).lastOption shouldBe Some("D")
  }

  it should "support map" in {
    new VirtualVector[String](Vector("A", "B", "C"), 1, Seq("D")).map(s => s) shouldBe Seq("A", "B", "D")
    new VirtualVector[String](Vector("A", "B", "C"), 1, Seq("D")).toSeq shouldBe Seq("A", "B", "D")
  }

  it should "support toString" in {
    new VirtualVector[String](Vector("A", "B", "C"), 1, Seq("D")).toString shouldBe "VirtualVector(A, B, D)"
  }
}
