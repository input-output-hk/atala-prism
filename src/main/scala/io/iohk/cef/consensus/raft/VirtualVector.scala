package io.iohk.cef.consensus.raft

class VirtualVector[T](base: IndexedSeq[T], deletes: Int, writes: Seq[T]) extends IndexedSeq[T] {
  override def length: Int = base.size - deletes + writes.size
  override def apply(idx: Int): T = {
    if (idx < base.size - deletes) {
      base(idx)
    } else {
      writes(idx - base.size + deletes)
    }
  }
}
