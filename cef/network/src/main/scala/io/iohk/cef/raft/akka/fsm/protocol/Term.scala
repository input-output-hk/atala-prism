package io.iohk.cef.raft.akka.fsm.protocol
//FIXME: use Ordered instead
final case class Term(termNo: Long) extends AnyVal {
  def prev: Term = this - 1
  def next: Term = this + 1

  def -(n: Long): Term = Term(termNo - n)
  def +(n: Long): Term = Term(termNo + n)

  def >(otherTerm: Term): Boolean = this.termNo > otherTerm.termNo
  def <(otherTerm: Term): Boolean = this.termNo < otherTerm.termNo

  def >=(otherTerm: Term): Boolean = this.termNo >= otherTerm.termNo
  def <=(otherTerm: Term): Boolean = this.termNo <= otherTerm.termNo
}

object Term {
  val Zero = Term(0)
}