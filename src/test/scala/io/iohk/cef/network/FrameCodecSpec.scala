package io.iohk.cef.network

import io.iohk.cef.network.NodeId.nodeIdBytes
import io.iohk.cef.network.encoding.nio.NativeCodecs
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.network.transport.{Frame, FrameDecoder, FrameEncoder, FrameHeader}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class FrameCodecSpec extends FlatSpec {

  private val genNodeId: Gen[NodeId] = listOfN(nodeIdBytes, arbitrary[Byte]).map(NodeId(_))

  private val genFrame: Gen[Frame[Int]] = for {
    src <- genNodeId
    dst <- genNodeId
    content <- arbitrary[Int]
  } yield Frame(FrameHeader(src, dst), content)

  private val genFrames: Gen[List[Frame[Int]]] = listOfN(2, genFrame)

  private val encoder = new FrameEncoder[Int](NativeCodecs.intEncoder)
  private val decoder = new FrameDecoder[Int](NativeCodecs.intDecoder)

  behavior of "FrameEncoder"

  forAll(genFrame) { frame: Frame[Int] =>
    it should s"encode and decode single frame $frame" in {
      decoder.decodeStream(encoder.encode(frame)) shouldBe Seq(frame)
    }
  }

  forAll(genFrames) { frames: List[Frame[Int]] =>
    it should s"encode and decode multiple frames: $frames" in {
      val buffs = frames.map(encoder.encode)
      val bigBuff = NetUtils.concatenate(buffs)
      decoder.decodeStream(bigBuff) shouldBe frames
    }
  }

}
