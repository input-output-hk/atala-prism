package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._

object DbConfigDao {

  def get(key: String): ConnectionIO[Option[String]] = {
    sql"""
    | SELECT value
    | FROM config
    | WHERE key = ${key}
    """.stripMargin.query[String].option
  }

  def getOrElse(key: String, value: String): ConnectionIO[String] =
    get(key).map(_.getOrElse(value))

  def setOrUpdate(key: String, value: String): ConnectionIO[Int] =
    sql"""
    | INSERT INTO config(key, value)
    | VALUES (${key}, ${value})
    | ON CONFLICT(key) DO UPDATE SET value = ${value}
    """.stripMargin.update.run

  def setIfNotExists(key: String, value: String): ConnectionIO[Int] =
    sql"""
    | INSERT INTO config(key, value)
    | VALUES (${key}, ${value})
    | ON CONFLICT(key) DO NOTHING
    """.stripMargin.update.run
}
