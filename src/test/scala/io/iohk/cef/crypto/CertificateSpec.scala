package io.iohk.cef.crypto

import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class CertificateSpec extends WordSpec {

  import Certificate._
  import CertificateSpec._

  "decode" should {
    "decode a PEM" in {
      val result = decode(pemWithCommonName).value

      result.commonName.value must be("Alexis Hernandez")
    }

    "ignore empty common name" in {
      val result = decode(pemWithoutCommonName).value

      result.commonName must be(empty)
    }

    "fail to decode an invalid pem" in {
      val result = decode(corruptedPem)

      result must be(empty)
    }

    "fail to decode an invalid input" in {
      val result = decode("j1")

      result must be(empty)
    }
  }
}

object CertificateSpec {

  /**
    * The following command could be used to generate a self-signed certificate:
    * - openssl req -x509 -newkey rsa:1024 -keyout key.pem -out cert.pem -days 36500 -nodes
    */
  val pemWithCommonName =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICYjCCAcugAwIBAgIJAPcirVjJ+hzEMA0GCSqGSIb3DQEBCwUAMEkxCzAJBgNV
      |BAYTAk1YMRAwDgYDVQQIDAdTaW5hbG9hMQ0wCwYDVQQKDARJT0hLMRkwFwYDVQQD
      |DBBBbGV4aXMgSGVybmFuZGV6MCAXDTE4MTEyOTIzMzU0NFoYDzIxMTgxMTA1MjMz
      |NTQ0WjBJMQswCQYDVQQGEwJNWDEQMA4GA1UECAwHU2luYWxvYTENMAsGA1UECgwE
      |SU9ISzEZMBcGA1UEAwwQQWxleGlzIEhlcm5hbmRlejCBnzANBgkqhkiG9w0BAQEF
      |AAOBjQAwgYkCgYEAq/2es5vt+f6vyOJe80BL908NMcLgwL22EmGcTo8hHXEqldnB
      |tR05onlood24zofo1M75agLA+ol050E83wGVcbxNQzQ9yuZE1pfgTf3kMADD5UHJ
      |WlpXvifnATUs6AG/FgisLifIAM4o0RoDeHaucErset2wK7uXOzOANXJLGl0CAwEA
      |AaNQME4wHQYDVR0OBBYEFEglvDPvlEKBTzE+otgYIK0j1qlhMB8GA1UdIwQYMBaA
      |FEglvDPvlEKBTzE+otgYIK0j1qlhMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEL
      |BQADgYEAha4xnMx/LlPreezmYbQb32PtFs+pxOL+SQKXWuE8dK/EgI9AqnT6dm03
      |9ydtpvQGh2BI26UxxaUKKG0sryFGVvZkc4vqSFt9V138Zm0RLoVS4jr9DxbfEp/h
      |2Xkeb6PH/p3fKpBLmN00Fv2XdvQPJy6HgZeN5qdv9cLO0oaw6Nw=
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val corruptedPem =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICYjCCAcugAwIBAgIJAPcirVjJ+hzEMA0GCSqGSIb3DQEBCwUAMEkxCzAJBgNV
      |BAYTAk1YMRAwDgYDVQQIDAdTaW5hbG9hMQ0wCwYDVQQKDARJT0hLMRkwFwYDVQQD
      |DBBBbGV4aXMgSGVybmFuZGV6MCAXDTE4MTEyOTIzMzU0NFoYDzIxMTgxMTA1MjMz
      |NTQ0WjBJMQswCQYDVQQGEwJNWDEQMA4GA1UECAwHU2luYWxvYTENMAsGA1UECgwE
      |SU9ISzEZMBcGA1UEAwwQQWxleGlzIEhlcm5hbmRlejCBnzANBgkqhkiG9w0BAQEF
      |AAOBjQAwgYkCGYEAq/2es5vt+f6vyOJe80BL908NMcLgwL22EmGcTo8hHXEqldnB
      |tR05onlood24zofo1M75agLA+ol050E83wGVcbxNQzQ9yuZE1pfgTf3kMADD5UHJ
      |WlpXvifnATUs6AG/FgisLifIAM4o0RoDeHaucErset2wK7uXOzOANXJLGl0CAwEA
      |AaNQME4wHQYDVR0OBBYEFEglvDPvlEKBTzE+otgYIK0j1qlhMB8GA1UdIwQYMBaA
      |FEglvDPvlEKBTzE+otgYIK0j1qlhMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEL
      |BQADgYEAha4xnMx/LlPreezmYbQb32PtFs+pxOL+SQKXWuE8dK/EgI9AqnT6dm03
      |9ydtpvQGh2BI26UxxaUKKG0sryFGVvZkc4vqSFt9V138Zm0RLoVS4jr9DxbfEp/h
      |2Xkeb6PH/p3fKpBLmN00Fv2XdvQPJy6HgZeN5qdv9cLO0oaw6Nw=
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val pemWithoutCommonName =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICKjCCAZOgAwIBAgIJAIDRcRzEjXqrMA0GCSqGSIb3DQEBCwUAMC4xCzAJBgNV
      |BAYTAk1YMRAwDgYDVQQIDAdTaW5hbG9hMQ0wCwYDVQQKDARJT0hLMB4XDTE4MTEy
      |OTIzMzgxNFoXDTE5MTEyOTIzMzgxNFowLjELMAkGA1UEBhMCTVgxEDAOBgNVBAgM
      |B1NpbmFsb2ExDTALBgNVBAoMBElPSEswgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJ
      |AoGBAMao8aiKMzhExgLI7X5gYfKVEQ3ek/rgne3l8i5S4rblaoUDvd1vnX/rRWwH
      |g3pxFMAKRZjkGNXC9yLr0QUlFhjgIPlNeqdPVpU9/pUKDWTMV2wTxPKMJGh0OnKq
      |nho4YjtZFoccVwM8+ED5XdT0UHVnqsZ7sfzRfxcK6HGurVwRAgMBAAGjUDBOMB0G
      |A1UdDgQWBBSedUQMK6Sdf9he6SGTDcyeYQ45DTAfBgNVHSMEGDAWgBSedUQMK6Sd
      |f9he6SGTDcyeYQ45DTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4GBAKcL
      |wKss+0LnLHPeqMCOZCn87L6V+tibtIgPvh67paXw4UNjJJ/CO3AORKmLs+MOvyYU
      |7gKuvyvvhH9KJOK9myTOdeiS5O68EXchxxVR82hN+FAhsTHkuklDf52EGqSIA1s6
      |t00zWJV2DMbGusegUHFpZNiaNnFJsPjT4jU4tjTZ
      |-----END CERTIFICATE-----
    """.stripMargin.trim
}
