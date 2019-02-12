package io.iohk.cef

import scala.util.Random
import java.nio.ByteBuffer

package object test {
  def randomBytes(n: Int): Array[Byte] = {
    val a = new Array[Byte](n)
    Random.nextBytes(a)
    a
  }

  def concatenate(buffs: Seq[ByteBuffer]): ByteBuffer = {
    val allocSize = buffs.foldLeft(0)((acc, nextBuff) => acc + nextBuff.capacity())

    val b0 = ByteBuffer.allocate(allocSize)

    (buffs.foldLeft(b0)((accBuff, nextBuff) => accBuff.put(nextBuff)): java.nio.Buffer).flip().asInstanceOf[ByteBuffer]
  }
}
