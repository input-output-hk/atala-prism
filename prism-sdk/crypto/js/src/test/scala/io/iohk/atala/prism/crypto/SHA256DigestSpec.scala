package io.iohk.atala.prism.crypto

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class Sha256DigestSpec extends AnyWordSpec {
  "Sha256Digest" should {
    "return the proper bytes array" in {

      /** The test cases in this file were created by running the code below
        * using the JVM library
        * for(i <- 0 until 10) {
        *   val input = Array.fill(20)((scala.util.Random.nextInt(256) - 128).toByte)
        *   println(s"val input$i = " + input.toVector.mkString("Array[Byte](",", ",")"))
        *   println(s"val expectedBytes$i = " + Sha256.compute(input).value.mkString("Vector[Byte](",", ",")"))
        *   println(s"""val expectedHex$i = """" + Sha256.compute(input).hexValue + """"""")
        *   println(s"\nval digest$i = Sha256.compute(input$i)")
        *   println(s"digest$i.value.toVector must be(expectedBytes$i)")
        *   println(s"""digest$i.hexValue must be(expectedHex$i)\n""")
        * }
        */

      val input0 = Array[Byte](108, 45, 2, -15, 19, 91, 57, 24, -116, 37, -72, -111, -4, -9, -35, 57, -116, 40, 23, 93)
      val expectedBytes0 = Vector[Byte](94, 54, 72, 119, 64, -127, 28, 87, -105, -67, 41, -56, -28, 51, 93, 36, -121,
        -18, 3, -75, -88, 100, 19, 44, -11, 55, -67, 83, 35, 89, -112, -97)
      val expectedHex0 = "5e36487740811c5797bd29c8e4335d2487ee03b5a864132cf537bd532359909f"

      val digest0 = Sha256.compute(input0)
      digest0.value.toVector must be(expectedBytes0)
      digest0.hexValue must be(expectedHex0)

      val input1 = Array[Byte](96, 6, -2, 13, -91, 107, 111, -5, -42, 49, 3, 46, -106, -120, -62, 46, 98, 42, 55, 21)
      val expectedBytes1 = Vector[Byte](-55, 16, -111, -29, 89, -91, 20, -27, 5, 97, 106, 13, -11, -105, 94, 127, -11,
        -19, 99, -87, -98, 31, -99, -22, 89, 58, -50, -13, -64, 10, 39, -17)
      val expectedHex1 = "c91091e359a514e505616a0df5975e7ff5ed63a99e1f9dea593acef3c00a27ef"

      val digest1 = Sha256.compute(input1)
      digest1.value.toVector must be(expectedBytes1)
      digest1.hexValue must be(expectedHex1)

      val input2 = Array[Byte](90, -20, 31, 47, -44, 116, -40, -66, 50, -52, -113, 73, 51, 49, 51, 47, -75, 4, -3, 38)
      val expectedBytes2 = Vector[Byte](24, 4, -63, 45, 34, -1, 27, 113, 126, 12, 118, -65, 84, 77, 108, 34, -34, -42,
        16, -67, 99, -95, -108, 13, 90, -52, -61, 65, -28, -3, -104, -51)
      val expectedHex2 = "1804c12d22ff1b717e0c76bf544d6c22ded610bd63a1940d5accc341e4fd98cd"

      val digest2 = Sha256.compute(input2)
      digest2.value.toVector must be(expectedBytes2)
      digest2.hexValue must be(expectedHex2)

      val input3 =
        Array[Byte](-67, 94, 25, 26, -117, -43, 48, -70, -123, -49, -122, 31, 108, -52, -82, 59, 85, -50, -79, -86)
      val expectedBytes3 = Vector[Byte](-21, 114, -26, 119, -120, -65, -104, -59, 3, 85, 12, -43, 114, 123, -63, 81,
        -73, -123, 63, 24, 102, 98, 76, -83, -126, 104, 8, 1, 2, -101, 34, 83)
      val expectedHex3 = "eb72e67788bf98c503550cd5727bc151b7853f1866624cad82680801029b2253"

      val digest3 = Sha256.compute(input3)
      digest3.value.toVector must be(expectedBytes3)
      digest3.hexValue must be(expectedHex3)

      val input4 = Array[Byte](-15, 106, -101, 56, -2, 12, 3, 16, -67, 117, 2, -54, 38, -5, 39, 82, -83, -59, -69, -50)
      val expectedBytes4 = Vector[Byte](-82, 65, 45, -128, -100, -10, -88, 32, -127, -102, 104, 94, 59, -49, 47, 116, 6,
        55, 7, -4, -39, -20, -11, -79, -113, -84, 120, 58, -104, 45, 121, -67)
      val expectedHex4 = "ae412d809cf6a820819a685e3bcf2f74063707fcd9ecf5b18fac783a982d79bd"

      val digest4 = Sha256.compute(input4)
      digest4.value.toVector must be(expectedBytes4)
      digest4.hexValue must be(expectedHex4)

      val input5 = Array[Byte](-21, 52, 105, -14, -82, -105, 5, 61, -23, -60, -49, 76, 8, 68, 80, 14, 111, -88, -31, 46)
      val expectedBytes5 = Vector[Byte](-88, -22, 32, -11, 117, -44, -123, 81, 122, -11, -93, -112, 103, -3, 122, -24,
        79, -59, 127, 0, 65, -86, 109, 18, 17, 74, 61, 72, -82, -5, 79, -46)
      val expectedHex5 = "a8ea20f575d485517af5a39067fd7ae84fc57f0041aa6d12114a3d48aefb4fd2"

      val digest5 = Sha256.compute(input5)
      digest5.value.toVector must be(expectedBytes5)
      digest5.hexValue must be(expectedHex5)

      val input6 =
        Array[Byte](4, 14, 24, -127, -31, -68, -18, -21, 5, 126, -77, 80, -71, -107, -92, -112, 90, 107, -27, 13)
      val expectedBytes6 = Vector[Byte](-78, -71, -89, -108, 80, 58, 66, -10, 83, -120, 94, -115, 54, 92, -20, -113, 74,
        -63, 77, -53, -71, 35, 97, -95, -124, -127, 1, -99, -21, -66, 50, 24)
      val expectedHex6 = "b2b9a794503a42f653885e8d365cec8f4ac14dcbb92361a18481019debbe3218"

      val digest6 = Sha256.compute(input6)
      digest6.value.toVector must be(expectedBytes6)
      digest6.hexValue must be(expectedHex6)

      val input7 =
        Array[Byte](-59, -90, 71, -61, 78, 81, -112, -29, -51, -12, 98, -97, 44, 22, 115, -3, -11, 105, -44, -5)
      val expectedBytes7 = Vector[Byte](21, 96, 69, 21, -14, -127, 106, -58, -78, 41, 48, 91, -84, -60, 75, -102, 53,
        63, -4, -118, -21, 81, 113, -73, -105, 92, 10, 111, 19, -104, 73, 88)
      val expectedHex7 = "15604515f2816ac6b229305bacc44b9a353ffc8aeb5171b7975c0a6f13984958"

      val digest7 = Sha256.compute(input7)
      digest7.value.toVector must be(expectedBytes7)
      digest7.hexValue must be(expectedHex7)

      val input8 =
        Array[Byte](88, -76, -30, 55, -16, -127, 68, -80, -52, -112, -88, -25, 36, -78, -107, -42, -65, 87, -80, 102)
      val expectedBytes8 = Vector[Byte](100, 10, 42, -68, 54, 15, -122, -62, 41, 4, -52, -62, 47, 87, 46, 127, -29,
        -124, 82, -62, -41, 78, -117, -50, 5, -34, 36, -82, 123, 81, 61, -86)
      val expectedHex8 = "640a2abc360f86c22904ccc22f572e7fe38452c2d74e8bce05de24ae7b513daa"

      val digest8 = Sha256.compute(input8)
      digest8.value.toVector must be(expectedBytes8)
      digest8.hexValue must be(expectedHex8)

      val input9 =
        Array[Byte](-34, 100, -95, 112, -118, -123, -8, 112, -79, -68, -63, -82, 83, 100, 93, -104, 47, -49, -65, -75)
      val expectedBytes9 = Vector[Byte](-89, -120, 115, 107, -118, 110, 71, 59, -102, 58, 3, 83, -15, -44, 109, 16, 15,
        -21, -98, 112, -15, -106, 96, 93, -42, 98, -27, 30, -94, 22, 111, -18)
      val expectedHex9 = "a788736b8a6e473b9a3a0353f1d46d100feb9e70f196605dd662e51ea2166fee"

      val digest9 = Sha256.compute(input9)
      digest9.value.toVector must be(expectedBytes9)
      digest9.hexValue must be(expectedHex9)
    }
  }

}
