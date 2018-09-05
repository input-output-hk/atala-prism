package io.iohk.cef.db.scalike
import scalikejdbc.{ConnectionPool, ConnectionPoolFactory, ConnectionPoolSettings}

object HikariConnPoolFactory extends ConnectionPoolFactory {

  override def apply(
      url: String,
      user: String,
      password: String,
      settings: ConnectionPoolSettings = ConnectionPoolSettings()): ConnectionPool = {
    new HikariConnPool(url, user, password, settings)
  }
}
