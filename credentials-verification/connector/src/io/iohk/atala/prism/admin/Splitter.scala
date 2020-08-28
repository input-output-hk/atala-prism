package io.iohk.atala.prism.admin

object Splitter {

  val (markerSpE, markerSpF) = ("\ufffe", "\uffff")

  def sqlSplit(str: String): Array[String] = split(str, ";", "\\")

  def split(str: String, sep: String, esc: String): Array[String] = {
    if (str.isEmpty) {
      Array.empty
    } else {
      val s0 = str.replace(esc + esc, markerSpE).replace(esc + sep, markerSpF)
      val s = if (s0.last.toString == esc) {
        s0.replace(esc, "") + esc
      } else {
        s0.replace(esc, "")
      }
      s.split(sep.head)
        .map(_.replace(markerSpE, esc + esc).replace(markerSpF, sep))
        .map(_.trim)
        .filter(_.nonEmpty)
    }
  }
}
