package io.iohk.cef

import akka.util.ByteString

package object utils {

  implicit class ByteStringExtension(val bytes: ByteString) extends AnyVal {

    /**
      * {{{
      *   >>> import akka.util.ByteString
      *   >>> val simpleHex = " 41 42 43"
      *   >>> ByteString("ABC").toHex == simpleHex
      *   true
      *
      *   >>> val multilineHex = """| 30 31 32 33 34 35 36 37 38 39 41 42 43 44 45 46
      *   ...                       | 30 31 32 33 34 35 36 37""".stripMargin
      *   >>> ByteString("0123456789ABCDEF01234567").toHex == multilineHex
      *   true
      *
      *   >>> val emptyString = ""
      *   >>> ByteString("").toHex == emptyString
      *   true
      *
      * }}}
      */
    def toHex: String = {
      def hex(b: Byte) = f" $b%02X"

      val builder = StringBuilder.newBuilder
      for (i <- 0 until bytes.size by 16) {
        if (i > 0) builder.append('\n')
        val line: Seq[Byte] = bytes.slice(i, i + 16)

        line.foreach { b =>
          builder ++= hex(b)
        }

      }

      builder.result()
    }
  }
}
