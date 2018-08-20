package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import io.iohk.cef.network.transport.tcp.NetUtils
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.verify
import org.mockito.Mockito.atLeastOnce

import scala.util.Random

class StreamCodecsSpec extends FlatSpec {

  object UserCode {
    case class A(i: Int, s: String)
    case class B(s: String)
    case class C(b: Boolean)
  }

  import UserCode._

  private val genA: Gen[A] = for {
    i <- arbitrary[Int]
    s <- arbitrary[String]
  } yield A(i, s)

  private val genB: Gen[B] = arbitrary[String].map(B)

  private val genC: Gen[C] = arbitrary[Boolean].map(C)

  private val genNetworkMess: Gen[List[Any]] = for {
    as <- listOf(genA)
    bs <- listOf(genB)
    cs <- listOf(genC)
  } yield Random.shuffle(as ::: bs ::: cs)

  private val aEnc = NioEncoder[A]
  private val bEnc = NioEncoder[B]
  private val cEnc = NioEncoder[C]

  private val aDec: NioDecoder[A] = NioDecoder[A]
  private val bDec: NioDecoder[B] = NioDecoder[B]
  private val cDec: NioDecoder[C] = NioDecoder[C]

  behavior of "StreamCodec"

  it should "decode a network message" in {
    forAll(genNetworkMess) { asBsAndCs =>

      val buffers = asBsAndCs.collect({
        case a: A => aEnc.encode(a)
        case b: B => bEnc.encode(b)
        case c: C => cEnc.encode(c)
      })
      val netBuffer = NetUtils.concatenate(buffers) // provides a random mix of As, Bs plus redundant C's as though 'on the wire'

      val decodedStream = decodeStream(netBuffer, Random.shuffle(List(aDec, bDec, cDec)))

      decodedStream shouldBe asBsAndCs
    }
  }

  it should "decode a network message and apply handlers" in {
    forAll(genNetworkMess) { asBsAndCs =>
      val buffers = asBsAndCs.collect({
        case a: A => aEnc.encode(a)
        case b: B => bEnc.encode(b)
        case c: C => cEnc.encode(c)
      })
      val netBuffer = NetUtils.concatenate(buffers) // provides a random mix of As, Bs plus redundant C's as though 'on the wire'

      val as: Seq[A] = asBsAndCs.collect({ case a: A => a })
      val bs: Seq[B] = asBsAndCs.collect({ case b: B => b })
      val cs: Seq[C] = asBsAndCs.collect({ case c: C => c })

      val aHandler: A => Unit = mock[A => Unit]
      val bHandler = mock[B => Unit]
      val cHandler = mock[C => Unit]

      val aDecoderFn = new DecoderFunction(aDec, aHandler)
      val bDecoderFn = new DecoderFunction(bDec, bHandler)
      val cDecoderFn = new DecoderFunction(cDec, cHandler)

      decodeStream2(netBuffer, Random.shuffle(List(aDecoderFn, bDecoderFn, cDecoderFn)))

      as.foreach(a => verify(aHandler, atLeastOnce()).apply(a))
      bs.foreach(b => verify(bHandler, atLeastOnce()).apply(b))
      cs.foreach(c => verify(cHandler, atLeastOnce()).apply(c))
    }
  }

  it should "not choke on empty buffers" in {
    decodeStream(ByteBuffer.allocate(0), List(aDec, bDec)) shouldBe Seq()
  }

}
