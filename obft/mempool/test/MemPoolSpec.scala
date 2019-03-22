package obft.mempool
package test

import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}
import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary

class MemPoolSpec extends FlatSpec with MustMatchers with EitherValues with PropertyChecks {

  behavior of "MemPool"

  it should "store the transactions it receives (in the same order it receives them)" in {
    forAll { (l: List[Int]) =>
      val mempool = new MemPool[Int](1)
      l.foreach(i => mempool.add(i))
      mempool.collect() mustBe l
    }
  }

  it should "store the transactions it receives (in the same order it receives them), even through multiple time slots" in {
    forAll { (l: List[Int]) =>
      val mempool = new MemPool[Int](l.length + 1)
      l.foreach { i =>
        mempool.add(i)
        mempool.advance()
      }
      mempool.collect() mustBe l
    }
  }

  // From the paper:
  // > The transaction is maintained in the mempool for u rounds, where u is a parameter
  it should "store transactions for u rounds" in {
    val tupleGen = for {
      u <- Gen.oneOf(1, 2, 3, 4, 10, 20)
      ls <- arbitrary[List[List[Int]]]
    } yield (u, ls)

    forAll(tupleGen) {
      case (u, ls) =>
        if (u > 0) {
          val mempool = new MemPool[Int](u)
          ls.zipWithIndex.foreach {
            case (l, idx) =>
              if (idx > 0)
                mempool.advance()
              l.foreach { i =>
                mempool.add(i)
              }
          }
          mempool.collect() mustBe ls.reverse.take(u).reverse.flatten
        }
    }
  }

}
