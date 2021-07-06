package io.iohk.atala.prism.utils

object OsUtils {
  def isLinux: Boolean = {
    val os = System.getProperty("os.name").toLowerCase
    os.contains("nix") || os.contains("nux") || os.contains("aix")
  }
}
