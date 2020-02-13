package io.iohk.cvp.admin

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class SplitterSpec extends FlatSpec {

  "ScriptSplitter" should "split on a semicolon, not on an escaped semicolon" in {
    val lines =
      """
        |
        |statement 1;
        |
        |statement 2 contains "\;";
        |
        |statement 3
        |""".stripMargin

    Splitter.split(lines, ";", "\\") shouldBe Array("statement 1", """statement 2 contains ";"""", "statement 3")
  }

  it should "return empty for empty" in {
    Splitter.split("", ";", "\\") shouldBe Array.empty
  }

  it should "work for an escaped escape" in {
    Splitter.split("""\\;\\""", ";", """\""") shouldBe Array("""\\""", """\\""")
  }
}
