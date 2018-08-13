package io.iohk.cef.ledger.chimeric

import scala.collection.mutable

case class Value(protected[Value] val m: Map[Currency, Quantity]) {
  require(m.forall(_._2 != BigDecimal(0)))

  def + (entry: (Currency, Quantity)): Value = {
    if (entry._2 == BigDecimal(0)) {
      this
    } else {
      Value(this.m + (entry._1 -> (this(entry._1) + entry._2)))
    }
  }
  def + (that: Value): Value = combineWithValue(_ + _, that)
  def unary_- : Value = new Value(m.mapValues(- _))
  def -(that: Value): Value = combineWithValue(_ - _, that)
  def apply(currency: Currency): Quantity = m.get(currency).getOrElse(BigDecimal(0))

  def >=(that: Value): Boolean = {
    (this.m.keys ++ that.m.keys).forall(at => {
      this (at) >= that(at)
    })
  }

  def iterator: Iterator[(Currency, Quantity)] = m.iterator


  private def combineWithValue(f: (Quantity, Quantity) => Quantity, that: Value): Value = combineWithPairs(f, that.m.toSeq)

  private def combineWithPairs(f: (Quantity, Quantity) => Quantity, values: Seq[(Currency, Quantity)]): Value = {
    //Using mutable for performance. Note that these side effects are not observable.
    //Referential transparency still holds for this method.
    val mutableM = mutable.Map(m.toSeq:_*)
    values.foreach{ case (curr, quant)  => {
      val newQuant = f(this(curr), quant)
      if(newQuant != BigDecimal(0)) {
        mutableM += (curr -> newQuant)
      } else {
        mutableM -= curr
      }
    }}
    new Value(mutableM.toMap)
  }
}

object Value {
  val Zero: Value = new Value(Map())
  def apply(values: (Currency, Quantity)*): Value = Value.Zero.combineWithPairs(_ + _, values)
}
