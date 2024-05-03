package io.iohk.atala.prism.node.utils

import io.lemonlabs.uri.config.UriConfig
import io.lemonlabs.uri.encoding.PercentEncoder
import io.lemonlabs.uri.{Uri, Url, Urn}

object UriUtils {

  private implicit val uriConfig: UriConfig = UriConfig.default.copy(queryEncoder = PercentEncoder())

  def isValidUriString(str: String): Boolean = {

    try {
      Uri.parse(str) match {
        case url: Url => url.schemeOption.nonEmpty
        case Urn(_) => true
      }
    } catch {
      case _: Exception => false
    }
  }

  /** Checks if a string is a valid URI fragment according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-3.5">RFC&nbsp;3986&nbsp;section-3.5</a>
    *
    * @param str
    * @return
    *   true if str is a valid URI fragment, otherwise false
    */
  def isValidUriFragment(str: String): Boolean = {

    /*
     * Alphanumeric characters (A-Z, a-z, 0-9)
     * Some special characters: -._~!$&'()*+,;=:@
     * Percent-encoded characters, which are represented by the pattern %[0-9A-Fa-f]{2}
     */
    val uriFragmentRegex = "^([A-Za-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9A-Fa-f]{2})*$".r

    // In general, empty URI fragment is a valid fragment, but for our use-case it would be pointless
    str.nonEmpty && uriFragmentRegex.matches(str)
  }
}
