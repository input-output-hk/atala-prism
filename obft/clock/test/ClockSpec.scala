package obft.clock
package test

// format: off

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalacheck.Gen

class ClockSpec extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks {

  // Quoting the paper:
  // > [...] by server i such that i - 1 = (j - 1) mod n
  "TimeSlot" should {
    "be able to report the current leader of each TimeSlot" in {
       val tupleGen = for {
         n <- Gen.posNum[Int]
         i <- Gen.chooseNum(1, n)
         j <- Gen.posNum[Int]
       } yield (n, i, j - 1)

       forAll(tupleGen) {
         case (n, i, j) =>
           whenever (n > 0 && i >= 1 && i <= n && j > 0) { // in theory tupleGen should not generate values that
                                                           // fail this check, but the shrinking process does

             if (i - 1 == (j - 1) % n) // 'i' should be the leader
               TimeSlot(j).leader(n) mustBe i
             else
               TimeSlot(j).leader(n) must not be i

           }
       }
    }
  }

}


