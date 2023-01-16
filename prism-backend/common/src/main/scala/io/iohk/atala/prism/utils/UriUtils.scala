package io.iohk.atala.prism.utils

import java.net.{URI, URLDecoder}

object UriUtils {

  // URL encoded triplet, a.k special character like %23 - '#' for example
  private[this] val tripletRegex = "%[0-9a-f]{2}".r

  // Characters that should never be encoded, according to https://www.rfc-editor.org/rfc/rfc3986#section-2.3
  private[this] val unreservedRegex = "[a-zA-Z0-9-._~]".r

  // Reserved characters that always need to be encoded
//  private[this] val reservedRegex = "[!*'();:@&=+$,/?#\\[\\] ]".r

  private[this] val genericUriRegex = "^\\w+:(\\/?\\/?)[^\\s]+$".r

  /** Normalized URI according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-6">RFC&nbsp;3986,&nbsp;section-6</a>
    *
    * @param uri
    * @return
    *   [[Some]](uri) - normalized uri, if it is a valid uri string, or [[None]]
    */
  def normalizeUri(uriStr: String): Option[String] = {

    // Characters that should never be encoded, according to https://www.rfc-editor.org/rfc/rfc3986#section-2.3
    try {
      if (genericUriRegex.matches(uriStr)) { // validate basic structure
        val pathSegmentNormalized = new URI(uriStr) // additional validation
          .normalize() // path segment normalization
          .toString

        val percentEncodingNormalized = tripletRegex.replaceAllIn(
          pathSegmentNormalized,
          m => {
            val encoded = m.group(0)
            // check if a triplet is encoded character that does not need to be encoded,
            // if it is, decode and replace, otherwise leave it as it is
            val decoded = URLDecoder.decode(encoded, "UTF-8")

            if (unreservedRegex.matches(decoded)) decoded
            else encoded
          }
        )

        val uri = new URI(percentEncodingNormalized)

        val scheme = uri.getScheme.toLowerCase
        val host = uri.getHost.toLowerCase

        val port = scheme match {
          case "http" =>
            if (uri.getPort == -1 || uri.getPort == 80) -1 else uri.getPort
          case "https" =>
            if (uri.getPort == -1 || uri.getPort == 443) -1 else uri.getPort
          case _ => uri.getPort
        }

        val path = "/+".r.replaceAllIn(uri.getPath, "/")

        val query = Option(uri.getQuery).map { q =>
          q.split('&')
            .collect { kv =>
              kv.split("=", 2) match {
                case Array(k, v) => k -> v
                case Array(k) => k -> ""
              }
            }
            .toMap // remove duplicates
            .toArray
            .sortBy(_._1) // sort alphabetically by key
            .map { case (k, v) =>
              s"$k=$v"
            }
            .mkString("&")
        }

        val normalized = new URI(
          scheme,
          uri.getUserInfo,
          host,
          port,
          path,
          query.orNull,
          uri.getFragment
        ).toString

        Some(normalized)

      } else None

    } catch {
      case m: Exception => Some(m.getMessage)
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
