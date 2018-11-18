package io.iohk.cef.db.scalike
import scalikejdbc._

trait SqlTable[Table] extends SQLSyntaxSupport[Table] with SqlFieldGetter[Table]
