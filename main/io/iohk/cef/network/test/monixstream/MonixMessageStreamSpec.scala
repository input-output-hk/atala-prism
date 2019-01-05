package io.iohk.cef.network.monixstream

import monix.reactive.Observable
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.listOfN
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class MonixMessageStreamSpec extends FlatSpec {

  private val distinctInts: Gen[List[Int]] = listOfN(2, arbitrary[Int]).map(_.distinct)
  private val distinctStrings: Gen[List[String]] = listOfN(2, arbitrary[String]).map(_.distinct)

  behavior of "MonixMessageStream"

  it should "execute foreach on a stream" in {
    forAll(distinctInts) { ls =>
      val ms = new MonixMessageStream(Observable.fromIterable(ls))
      val f = mock[Int => Unit]

      ms.foreach(i => f(i)).futureValue

      ls.foreach(l => verify(f)(l))
    }
  }

  it should "map a stream to a new value" in {
    forAll(distinctInts) { ls =>
      val ms = new MonixMessageStream(Observable.fromIterable(ls))
      val f = mock[String => Unit]

      ms.map(i => i.toString).foreach(s => f(s)).futureValue

      ls.foreach(l => verify(f)(l.toString))
    }
  }

  it should "filter elements from a stream" in {
    forAll(distinctStrings) { strings =>
      val stringStream = new MonixMessageStream(Observable.fromIterable(strings))
      val f = mock[String => Unit]
      val nonEmpty: String => Boolean = s => s.nonEmpty

      stringStream.filter(nonEmpty).foreach(f)

      strings.filter(nonEmpty).foreach(s => verify(f).apply(s))
      verifyNoMoreInteractions(f)
    }
  }

  it should "fold a stream to produce a value" in {
    forAll(distinctInts) { ints =>
      val intStream = new MonixMessageStream(Observable.fromIterable(ints))

      val streamSum = intStream.fold(0)((acc, next) => acc + next).futureValue

      streamSum shouldBe ints.sum
    }
  }
}
