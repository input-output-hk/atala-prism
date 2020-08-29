package io.iohk.atala.prism.utils

import doobie.util.Meta

import scala.collection.compat.immutable.ArraySeq

object DoobieImplicits {
  implicit val byteArraySeqMeta: Meta[ArraySeq[Byte]] = Meta[Array[Byte]].timap {
    ArraySeq.unsafeWrapArray
  } { _.toArray }
}
