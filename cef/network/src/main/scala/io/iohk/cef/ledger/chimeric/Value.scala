package io.iohk.cef.ledger.chimeric

import scala.collection.mutable

case class Value(protected[Value] val m: Map[Currency, Quantity]) {
  require(m.forall(_._2 != BigDecimal(0)))

  def + (entry: (Currency, Quantity)) = Value(this.m + entry)
  def + (that: Value): Value = combine(that, _ + _)
  def unary_- : Value = new Value(m.mapValues(- _))
  def -(that: Value): Value = this + (- that)
  def apply(currency: Currency): Quantity = m.get(currency).getOrElse(BigDecimal(0))

  def >=(that: Value): Boolean = {
    (this.m.keys ++ that.m.keys).forall(at => {
      this (at) >= that(at)
    })
  }

  def iterator: Iterator[(Currency, Quantity)] = m.iterator

  private def combine(that: Value, f: (Quantity, Quantity) => Quantity): Value = {
    //Using mutable for performance. Note that these side effects are not observable.
    //Referential transparency still holds for this method.
    val mutableM = mutable.Map(m.toSeq:_*)
    mutableM.foreach{ case (curr, quant)  => {
      val newQuant = f(quant, that(curr))
      if(newQuant != BigDecimal(0)) {
        mutableM += (curr -> newQuant)
      } else {
        mutableM -= curr
      }
    }}
    val keysToAdd = that.m.keySet diff m.keySet
    keysToAdd.foreach(curr => mutableM += (curr -> that(curr)))
    new Value(mutableM.toMap)
  }
}

object Value {
  val Zero: Value = new Value(Map())
  //If currency c is present in more than one pair, the last element where currency==c will remain as c's quantity
  def apply(values: (Currency, Quantity)*): Value =
    new Value(Map(values.filter(_._2 != 0):_*))
}
