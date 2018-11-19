package io.iohk.cef.db.scalike
import io.iohk.cef.data.query.Value
import io.iohk.cef.data.query.Value._
import scalikejdbc.ParameterBinderFactory

/**
  * We need to teach ScalikeJDBC how to bind parameters of type Ref in a safe way.
  * That's the purpose of this object.
  */
object ParameterBinderFactoryImplicits {

  implicit val parameterBinderFactoryAnyVal: ParameterBinderFactory[Value] = createParameterBinderFactoryAnyVal

  def createParameterBinderFactoryAnyVal(
      implicit pInt: ParameterBinderFactory[Int],
      pDouble: ParameterBinderFactory[Double],
      pFloat: ParameterBinderFactory[Float],
      pLong: ParameterBinderFactory[Long],
      pShort: ParameterBinderFactory[Short],
      pByte: ParameterBinderFactory[Byte],
      pBoolean: ParameterBinderFactory[Boolean],
      pString: ParameterBinderFactory[String]): ParameterBinderFactory[Value] =
    value =>
      value match {
        case DoubleRef(value) => pDouble(value)
        case FloatRef(value) => pFloat(value)
        case LongRef(value) => pLong(value)
        case IntRef(value) => pInt(value)
        case ShortRef(value) => pShort(value)
        case ByteRef(value) => pByte(value)
        case BooleanRef(value) => pBoolean(value)
        case CharRef(value) => pString(value.toString)
        case StringRef(value) => pString(value)
    }
}
