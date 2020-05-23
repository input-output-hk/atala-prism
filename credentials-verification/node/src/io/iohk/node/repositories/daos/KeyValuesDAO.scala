package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._

object KeyValuesDAO {
  case class KeyValue(key: String, value: Option[String])

  def upsert(keyValue: KeyValue): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO key_values (key, value)
         |VALUES (${keyValue.key}, ${keyValue.value})
         |ON CONFLICT (key)
         |DO UPDATE SET value = ${keyValue.value}
      """.stripMargin.update.run.map(_ => ())
  }

  def get(key: String): ConnectionIO[Option[KeyValue]] = {
    sql"""
         |SELECT key, value
         |FROM key_values
         |WHERE key = $key
       """.stripMargin.query[KeyValue].option
  }
}
