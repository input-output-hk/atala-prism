package io.iohk.atala.cvp.webextension.util

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers._

class ByteOpsSpec extends AnyWordSpec {
  "convertBytesToHex" should {
    "work for all bytes" in {
      (0 to 255).map(_.toByte).map(Array(_)).foreach { input =>
        val result = ByteOps.convertBytesToHex(input)
        val expected = hackyHexParser(result)
        expected must be(input)
      }
    }
  }

  // this is just a way to verify that our results are correct
  private def hackyHexParser(string: String): Array[Byte] = {
    val bytes = BigInt(string, 16).toByteArray
    if (bytes.length > 1 && bytes.headOption.contains(0.toByte)) {
      bytes.drop(1)
    } else {
      bytes
    }
  }
}
