import sbt._

case class FlywayConfig(url: String, user: String, password: String)

object FlywayConfig {
  lazy val config: SettingKey[FlywayConfig] = settingKey("Flyway config")
}
