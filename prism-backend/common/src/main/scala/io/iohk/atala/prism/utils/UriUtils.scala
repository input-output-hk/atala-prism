package io.iohk.atala.prism.utils
import java.net.{URI, URISyntaxException, URLEncoder, URLDecoder}

object UriUtils

  // URL encoded triplet, a.k special character like %23 - '#' for example
  private[this] val tripletRegex = "%[0-9a-f]{2}".r

  // Characters that should never be encoded, according to https://www.rfc-editor.org/rfc/rfc3986#section-2.3
  private[this] val unreservedRegex = "\"[a-zA-Z0-9-._~]\".r"

  /** Checks if a string is a valid URI
    *
    * @param uri
    * @return
    */
  def isValidUri(uri: String): Boolean = {
    try {
      new URI(uri)
      true
    } catch {
      case _: URISyntaxException => false
    }
  }

  /** Normalized URI according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-6">RFC&nbsp;3986,&nbsp;section-6</a>
    *
    * @param uri
    * @return
    *   [[Some]](uri) - normalized uri, if it is a valid uri string, or [[None]]
    */
  def normalizeUri(uri: String): Option[String] = {

    def pathSegmentNormalization(uri: URI): URI = {
      // Algorithm performed is described here: https://javadoc.scijava.org/Java11/java.base/java/net/URI.html#normalize()
      uri.normalize()
    }

    def caseNormalization(uri: URI): URI = {
      // Rules described here: https://www.rfc-editor.org/rfc/rfc3986#section-6.2.2.1

      val scheme = uri.getScheme.toLowerCase // schema to lower case
      val host = uri.getHost.toLowerCase // host to lowercase

      /*
       * the hexadecimal digits within a percent-encoding
       * triplet (e.g., "%3a" versus "%3A") are case-insensitive and therefore
       * should be normalized to use uppercase letters for the digits A-F
       *
       * This includes encoded characters in path, query and fragment parts of the URI
       */

      val path = tripletRegex.replaceAllIn(uri.getPath, _.group(0).toUpperCase)
      val query = uri.getQuery match {
        case null => null
        case q => tripletRegex.replaceAllIn(q, _.group(0).toUpperCase)
      }
      val fragment = uri.getFragment match {
        case null => null
        case f => tripletRegex.replaceAllIn(f, _.group(0).toUpperCase)
      }
      val normalized = new URI(scheme, uri.getUserInfo, host, uri.getPort, path, query, fragment)

      normalized
    }

    def percentEncodingNormalization(uri: URI): URI = {
      // Rules described here: https://www.rfc-editor.org/rfc/rfc3986#section-6.2.2.2

      tripletRegex.replaceAllIn(uri.toString, m => {
        val encoded = m.group(0)
        // check if a triplet is encoded character that does not need to be encoded,
        // if it is, decode and replace, otherwise leave it as it is
        val decoded = URLDecoder.decode(encoded, "UTF-8")

        if (unreservedRegex.matches(decoded)) decoded
        else encoded
      })

    }

    try {
      val normalizedUri = new URI(uri).normalize()

      Some(normalizedUri.toString)
    } catch {
      case _: URISyntaxException => None
    }

    Some(uri)
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

