package io.iohk.atala.prism.utils

import java.net.{URI, URL, URISyntaxException, MalformedURLException, URLEncoder, URLDecoder}

object UriUtils {

  // URL encoded triplet, a.k special character like %23 - '#' for example
  private[this] val tripletRegex = "%[0-9a-f]{2}".r

  // Characters that should never be encoded, according to https://www.rfc-editor.org/rfc/rfc3986#section-2.3
  private[this] val unreservedRegex = "[a-zA-Z0-9-._~]".r

  // Reserved characters that always need to be encoded
  private[this] val reservedRegex = "[!*'();:@&=+$,/?#[\\] ]".r

  private[this] val genericUriRegex = "^\\w+:(\\/?\\/?)[^\\s]+$".r

  /** Normalized URI according to <a
    * href="https://www.rfc-editor.org/rfc/rfc3986#section-6">RFC&nbsp;3986,&nbsp;section-6</a>
    *
    * @param uri
    * @return
    *   [[Some]](uri) - normalized uri, if it is a valid uri string, or [[None]]
    */
  def normalizeUri(uri: String): Option[String] = {

    val pathSegmentNormalization: URI => URI = (uri) => {
      // Algorithm performed is described here: https://javadoc.scijava.org/Java11/java.base/java/net/URI.html#normalize()
      uri.normalize()
    }

    val caseNormalization: URI => URI = (uri) => {
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

    val percentEncodingNormalization: URI => URI = (uri) => {
      // Rules described here: https://www.rfc-editor.org/rfc/rfc3986#section-6.2.2.2

      val normalized = tripletRegex.replaceAllIn(
        uri.toString,
        m => {
          val encoded = m.group(0)
          // check if a triplet is encoded character that does not need to be encoded,
          // if it is, decode and replace, otherwise leave it as it is
          val decoded = URLDecoder.decode(encoded, "UTF-8")

          if (unreservedRegex.matches(decoded)) decoded
          else encoded
        }
      )

      new URI(normalized)

    }

    val httpSpecificNormalization: URI => URI = (uri) => {

      val url = new URL(uri.toString) // performs URL specific validation
      val scheme = url.getProtocol

      // remove unnecessary port
      val port = scheme match {
        case "http" =>
          if (url.getPort == -1 || url.getPort == 80) -1 else url.getPort
        case "https" =>
          if (url.getPort == -1 || url.getPort == 443) -1 else url.getPort
        case _ => -1
      }

      val path = {
        // remove unnecessary forward slashes in path
        val noExtraSlashes = "/+".r.replaceAllIn(url.getPath, "/")

        // encode special characters in path
        val encoded = noExtraSlashes
          .split('/')
          .map(segment =>
            reservedRegex.replaceAllIn(
              segment,
              m => URLEncoder.encode(m.group(0), "UTF-8")
            )
          )
          .mkString("/")

        encoded
      }

      // remove duplicates, sort query params alphabetically, and encode reserved characters if any
      val query = Option(url.getQuery).map { q =>
        q.split('&')
          .map { kv =>
            val Array(k, v) = kv.split("=", 2)
            k -> v
          }
          .toMap // remove duplicates
          .toArray
          .sortBy(_._1) // sort alphabetically by key
          .map { case (k, v) => // encode reserved characters in key and value
            val encodedKey = reservedRegex.replaceAllIn(k, m => URLEncoder.encode(m.group(0), "UTF-8"))
            val encodedVal = reservedRegex.replaceAllIn(v, m => URLEncoder.encode(m.group(0), "UTF-8"))
            s"$encodedKey=$encodedVal"
          }
          .mkString("&")
      }

      val normalized = new URI(scheme, url.getUserInfo, url.getHost, port, path, query.orNull, uri.getFragment)

      normalized

    }

    val normalize = pathSegmentNormalization andThen caseNormalization andThen percentEncodingNormalization

    try {
      if (genericUriRegex.matches(uri)) {
        val uriObj = new URI(uri)

        val zNormalize =
          if (uriObj.getScheme == "http" || uriObj.getScheme == "https") normalize andThen httpSpecificNormalization
          else normalize

        val normalized = zNormalize(uriObj).toString

        Some(normalized)
      } else None

    } catch {
      case _: URISyntaxException | _: MalformedURLException => None
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
