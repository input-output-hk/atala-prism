package io.iohk.atala.prism.utils

/** Helper functions to deal with strings.
  *
  * NOTE: This isn't called StringOps to prevent name clashes with the stdlib.
  */
object StringUtils {

  /** Masks a string, usually used to print secrets in logs.
    *
    * The masking is done by taking the 2 characters from the prefix, and 2 from the suffix, the rest gets changed to
    * asterisks (*). When there aren't enough characters, everything is masked.
    *
    * For example:
    *   - masked("abcdef") == "ab**ef"
    *   - masked("abcd") == "****"
    */
  def masked(string: String): String = {
    if (string.length <= 4) {
      "*" * string.length
    } else {
      val mask = "*" * (string.length - 4)
      s"${string.take(2)}$mask${string.takeRight(2)}"
    }
  }
}
