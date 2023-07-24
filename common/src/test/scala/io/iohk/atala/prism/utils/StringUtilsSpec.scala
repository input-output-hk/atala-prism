package io.iohk.atala.prism.utils

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class StringUtilsSpec extends AnyWordSpec {
  "mask" should {
    "mask the whole string when there are less than 4 characters" in {
      val cases = List(
        "",
        "a",
        "ab",
        "abc",
        "abcd"
      )

      cases.foreach { input =>
        val result = StringUtils.masked(input)
        result.length must be(input.length)
        result.forall(_ == '*') must be(true)
      }
    }

    "longer words get a prefix/suffix unmasked" in {
      val cases = List(
        "abcde" -> "ab*de",
        "abcdef" -> "ab**ef",
        "abcdefg" -> "ab***fg"
      )
      cases.foreach { case (input, expected) =>
        val result = StringUtils.masked(input)
        result must be(expected)
      }
    }
  }
}
