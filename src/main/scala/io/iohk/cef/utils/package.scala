package io.iohk.cef

import akka.util.ByteString

package object utils {

  // Borrowed from here: https://gist.github.com/mkiedys/4c431cfbe20add51e984
  implicit class ByteStringExtension(val bytes: ByteString) extends AnyVal {
    def hexDump: String = {
      def pos(i: Int) = f"$i%08x"
      def hex(b: Byte) = f" $b%02x"

      val builder = StringBuilder.newBuilder
      for (i <- 0 until bytes.size by 16) {
        val line = bytes.slice(i, i + 16)
        val (l8, r8) = line.splitAt(8)

        builder.append(pos(i))
        builder.append(' ')
        for (b <- l8) builder.append(hex(b))
        builder.append(' ')
        for (b <- r8) builder.append(hex(b))

        for (_ <- line.size to 16 * 3) builder.append(' ')

        builder.append('|')
        for (b <- line)
          if (b >= 32 && b <= 126) builder.append(b.toChar)
          else builder += '.'
        builder.append('|')
        builder.append('\n')
      }

      if (bytes.size % 16 != 0)
        builder
          .append(pos(bytes.size))
          .append('\n')

      builder.result()
    }
  }
}
