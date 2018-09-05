package io.iohk.cef.db.scalike
import java.sql.Connection

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import scalikejdbc.{ConnectionPool, ConnectionPoolSettings}

class HikariConnPool(url: String, user: String, password: String, settings: ConnectionPoolSettings)
    extends ConnectionPool(url, user, password, settings) {

  private val ds = {
    val config = new HikariConfig()
    config.setJdbcUrl(url)
    config.setUsername(user)
    config.setPassword(password)
    config.setConnectionTestQuery(settings.validationQuery)
    config.setMaximumPoolSize(settings.maxSize)
    config.setConnectionTimeout(settings.connectionTimeoutMillis)
    new HikariDataSource(config)
  }

  override def dataSource: DataSource = ds
  override def borrow(): Connection = ds.getConnection()
  override def numActive: Int = ds.getHikariPoolMXBean.getActiveConnections
  override def numIdle: Int = ds.getHikariPoolMXBean.getIdleConnections
  override def maxActive: Int = ds.getMaximumPoolSize
  override def maxIdle: Int = ds.getMaximumPoolSize
  override def close(): Unit = ds.close()
}
