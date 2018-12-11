package io.iohk.cef.crypto.certificates

import java.io.ByteArrayInputStream
import java.security.cert.{CertificateFactory, X509Certificate}

object ExampleCertificates {

  private val factory = CertificateFactory.getInstance("X.509")

  def decodeX509(pem: String): X509Certificate = {
    factory
      .generateCertificate(new ByteArrayInputStream(pem.getBytes))
      .asInstanceOf[X509Certificate]
  }

  val enterpriseCA =
    """
      |-----BEGIN CERTIFICATE-----
      |MIIC1TCCAb2gAwIBAgIEXAtR6zANBgkqhkiG9w0BAQsFADAcMQswCQYDVQQGEwJI
      |SzENMAsGA1UEAwwESU9ISzAgFw0wMDEyMDgwNTA4NTlaGA8yMDYwMTIwODA1MDg1
      |OVowHDELMAkGA1UEBhMCSEsxDTALBgNVBAMMBElPSEswggEiMA0GCSqGSIb3DQEB
      |AQUAA4IBDwAwggEKAoIBAQDbkYWD2fvI/2VpXg9uqxWQ7hf5B4m0Kkld6+QKgUY8
      |FGBsX9hJBdkdvXk9EutILOtof1bXuMTRlTwBN/WOrKUfTOSi5I7A+lXUggkrTa1i
      |U+SjAmx2UZ3GSjefIFpb7e1AOyrIiiT9AY0F0QiScPF0LSZ6OzwWWYqwmkk5h7yT
      |pFtesh39vSnXZzGIxRzkAkqbDKnOQ7hQ+w+dvpgKXdU4fI2oF0Q3hNWuqepMJ3eA
      |0y9NjK3+hhGYVQBxhu2Hj4EabF9scHkg1L4zaWZ4gf42luMvbOLFy5/mF0P3wkgi
      |8YZBSpW9uOfDysvpTmQc69lPYuSEH+cxnFqLmK5f+go3AgMBAAGjHTAbMAwGA1Ud
      |EwQFMAMBAf8wCwYDVR0PBAQDAgKEMA0GCSqGSIb3DQEBCwUAA4IBAQBgo+sUJQcl
      |6P/5xI+ExIsvCSfZWiTiVloCUwBvdBSwNo62ZdwPMXEjSiYxu9TLZVts9uK2hlpK
      |FQd/ZN0NTxTFskKdSXgy+jUm5kIU8i6D8eupyiPfqyMv7z7g4iUOSEXToCfCcLDu
      |2MkomJilCmxJlEn+Z+QbTxxYJ3U/EgAIKj7pWAsRfHhC6j7+vNMXvOgI8dnu0mDk
      |VhjOw8JsgJYuINnavKovPbWlkhvumGjwx0xeSAAO4lAgyUmHnyq10PDsAlRsBy60
      |su8O6VY8QSYVy8VO3pJ5wJiPZIOLloRciWZIH1z/IrWswe0NizOZXBUldptYgdAB
      |rGjeu8HxcmIi
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val externalCA =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICwzCCAaugAwIBAgIEXAtS/jANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDDAhl
      |eHRlcm5hbDAgFw0wMDEyMDgwNTEzMzRaGA8yMDYwMTIwODA1MTMzNFowEzERMA8G
      |A1UEAwwIZXh0ZXJuYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDT
      |c7m7Y4jSr4cBAoic3bfsN9OtaZLufNLYzzQVPNtsU0zP6f/JEmCfeN9ACvZgkgaI
      |z+IsquWixkHGWFFQHOXWrnT25nWpqqlCscjWS/bcgiYrnclMP4IyfXXq5stTUD+V
      |lS2P3b8sU4pM7ELLVDP23NM89bLf5TmURZ+OK5Cbu8neasszJRihJUv1qNs2Z5yS
      |YuHfj7omgf02ImvCNXOSNb/xRbJNq0AlfZr84o8aeVbshD4yr4avYKL2EBjlKsom
      |nf/+oLriIuqWkUJ4MAPEa7v7mv5mxn81l7+8Ja6tD/P8Lq4kPQUZuByrLTDPc6AC
      |7jy2rFBs3J0keCu1shQ5AgMBAAGjHTAbMAwGA1UdEwQFMAMBAf8wCwYDVR0PBAQD
      |AgKEMA0GCSqGSIb3DQEBCwUAA4IBAQCOW3pGQjpePluOTD4GIT8caRR3Vy09GU6I
      |jBrG+WzKecngMYMMgxcYvtdMV89oJGuE0gLSPy41AUE8tCxiC8GTxLIYs0Svz92t
      |nZt6OsZZ0f+s26YtX68eXuno6xSjQv0HuWIkvCwa8v+73TczktPw7AQqR3XNWXOP
      |65V9Mk14Zl5DisohGG+ABhrAf9QUd5QAVoslguIiOJLe3YH7NnCrpUr9X2DPNgpL
      |U1mw00gnpmowNWrGFzp4IqEnefjVmIrhp7UsRTD+5rzoJMOsiMGqergJRy0JEj/V
      |q2m2Pl9gK4rNJdow9Xnzw9hCBT5T2r2kpVWeq+Keg8Pb5M4c0aV7
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val validCert =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICqjCCAZKgAwIBAgIEXAtTSTANBgkqhkiG9w0BAQsFADAcMQswCQYDVQQGEwJI
      |SzENMAsGA1UEAwwESU9ISzAgFw0xODEyMDgwNTE0NDlaGA8yMDUwMTIwODA1MTQ0
      |OVowEDEOMAwGA1UEAwwFdmFsaWQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
      |AoIBAQDk11L4C2IdRaRrRzIUytNeNFZw0MRZ9C2xGc1ksDFz5/XJm5As8F94HNsm
      |I3xdu1q1iGC9wan0twqw2FCdcRmnpmepuor2ufZ6xp+Q/Gi4Ahf/S8R5M3uCnXSW
      |s3N6jM4zNHdDxFSRzrHZVVyFE/oI71xoVg1ui1G4L7bIImBhBc0lWETThQKK52ss
      |FFEYo8eG73tL/KvW3fq1jf+8wr1r7nQA2xfr/3XeQNntlAT3q7IH5YcIgjHg4ZZu
      |tVfP9rppqL/xd/3q+/P/dVht8OfVnYej1IxO1Sr2rzWhLsmierWx7pCfbZeB6smw
      |ok7vWnZOnOMju2Zm84j8FaVVtIV3AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAMnv
      |XDi64qkk9HmfAsBBM2/2NWLEwe8vnOHMFsiJJQhFWcC39MNjC1+EPbV81m3Jz2Je
      |V7MyzKWs1WQF2tpbuE3xe8wVIIiL6O/Gr5U9pW20fYs8PHoNMWYatj7oigkYWD4n
      |cnVtFxUuTro86gdj/uXFI/1lBw/W3EEexa1cNAO1Up2b9FsPpci1iSGN7gXkbmRA
      |isCMj3CGkFxzjqaXYJoFhXfSi683UX+I14MBKE/bwj0oeEMtmyHQ8LaWm9GTbcJ0
      |01tF+Yits/jjeXAEOT3vhxzgOyWWJs8CGqu7H3Uf5eqohuDkqtJRjoOgzH7Zem9Q
      |gezppWASH86vRZmiF5o=
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val expiredCert =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICqjCCAZKgAwIBAgIEXAtTeDANBgkqhkiG9w0BAQsFADAcMQswCQYDVQQGEwJI
      |SzENMAsGA1UEAwwESU9ISzAeFw0xMDEyMDgwNTE1MzZaFw0xNzEyMDgwNTE1MzZa
      |MBIxEDAOBgNVBAMMB2V4cGlyZWQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
      |AoIBAQCx4YFUyoqC9oxhpA+X34Ckb4nIAJhv+o5VhyUlRs+ordysLV2pOcjTDrq3
      |Lhz1FXkYuGfAcMo90Og4CcehOzar5sOxjByJDEHBT2jI5/F9ilCFf30Vw835Qpy+
      |/kMKqoh9YMhfRonc3MJLOTR45JprFilnsHHMW8rvvKxZNbaeevPvWSIxq47ws86H
      |lW5U9Dm1QVNLVku6ZiIGv5WkCwJp9iNMWdbM3oitZAg2b7p0dX3VQntu06a/qz0m
      |9dp3zxsA4u6hYmUSz4e+BOsVI+2cqlFCEiVEk4ina0NIpqcAfxs381uQTtjoMju8
      |qFCd8N60U+8CmHp3ElIzyshT+e1xAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAHXq
      |uWlRTSW9shCBtF9KnvwbLKMOWy6CWEQGk7VI4fIzIrt4aer+i2KhVlUPVkrIAH4N
      |Kfx1nIj0OhJ0+rNgC5jXHuQY65DLx7uwpIj//bvuIPfSzXwUNIjEEnZmj1/KrsId
      |eNZxZZ1QkRJp3m9BqcllxVDLK0ILGwAOJbrV9O3hsGnPBXMJfYJp7Y+wSICxE6UR
      |9/vwJ5SKnQ+M62Yirle/NK/0+1e7C923cVq82UXc/0YNgpSENx9Eotquaj5XG2R5
      |C8rneHhK9JhZsB1h/qLZGF1e7E3IPiwhTksBKUcmocACANoxZrCUsWmPt9bbInLG
      |ImJVIB87QSwNcsjsA/0=
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val notValidYetCert =
    """
      |-----BEGIN CERTIFICATE-----
      |MIICsDCCAZigAwIBAgIEXAtTsTANBgkqhkiG9w0BAQsFADAcMQswCQYDVQQGEwJI
      |SzENMAsGA1UEAwwESU9ISzAgFw00MDEyMDgwNTE2MzNaGA8yMDUwMTIwODA1MTYz
      |M1owFjEUMBIGA1UEAwwLbm90VmFsaWRZZXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
      |DwAwggEKAoIBAQCFEDZdVpNtzj9L8O4DWDPvDfAJptFKrgjkF14udrqwXiT+KFf8
      |GbdsnodBYBn8Ag0v5FomwHaNfHgLW1jxiBtQRCoDfkBDzpN4Xz//zY/z209QH1yr
      |bDNTgzOKFoSiOKAEXF6J0lDhkNFWKrAgwJoLsBzZtKmg7hD4ooJ0HyLRCBss18gc
      |qM/Nyt1zUc4PUJ8hfR2OqWS0eJEgGujIDAaJpBY6tZL45iWEiLNS1PFC310E2C5p
      |6IBaI6QC3SgOSJByATZXop2crp8HOymLj4dvYDheYJ9xo/qIXU3j1qLsFUYSwgUp
      |76eeAjfs45GaN8bv+uGDV4lfq+LSjwIlzkzJAgMBAAEwDQYJKoZIhvcNAQELBQAD
      |ggEBAEqZmHWGSceul8HxExhEIKJV1OJNXXjsYtNoJrSyFsWy7atoA1J3VfdfiFPP
      |mYQn4zoC0IzuoNr8+dTSxKICbadwpNEx/wcy3wRdsLna1LwNj3T5RHtbYv3klT+8
      |BMCnEaywF6sRrs+uR8WVwvKs/RrWo7VTyDFzaZNGsMJaOy+Adr0XIDuWTbVNfuNd
      |pPOw7nRP9z7DpIjxCgTCX/TzxO87H6U1+6hgPXTaAJ7/PLbR9qFmi69e9hzQOlwZ
      |OWSro9FNdLRmydkJ6/+o0aMkMlAmSbQuBrNTnOON3ociYR+v5xjJSxPtKytq5YOk
      |onixQmQmBYHJd+0l9gUCUB2hE3k=
      |-----END CERTIFICATE-----
    """.stripMargin.trim

  val singleCertWithCommonNamePEM = validCert

  val singleCertWithoutCommonNamePEM =
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

  val twoChainedCertsPEM =
    s"""
       |$validCert
       |$enterpriseCA
    """.stripMargin.trim

  val twoUnchainedCertsPEM =
    s"""
       |$validCert
       |$externalCA
    """.stripMargin.trim

  val twoChainedCertsNotValidYetPEM =
    s"""
       |$notValidYetCert
       |$enterpriseCA
    """.stripMargin

  val twoChainedCertsExpiredPEM =
    s"""
       |$expiredCert
       |$enterpriseCA
    """.stripMargin
}
