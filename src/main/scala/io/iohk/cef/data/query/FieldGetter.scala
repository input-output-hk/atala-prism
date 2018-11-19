package io.iohk.cef.data.query

trait FieldGetter[LowLvlFieldType] {
  def getField(field: Field): LowLvlFieldType
}
