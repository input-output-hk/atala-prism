package identus

package object apollo {
  type MyPublicKey = PrivateKey

  object MyPublicKey {
    def apply(curve: String, x: Array[Byte], y: Array[Byte]): MyPublicKey = ???
    def apply(curve: String, data: Array[Byte]): MyPublicKey = {
      curve match {
        case "secp256k1" =>
          val companion = new io.iohk.atala.prism.apollo.utils.KMMECSecp256k1PublicKey.Companion
          val secp256k1PublicKey = companion.secp256k1FromBytes(data)
          val point = secp256k1PublicKey.getCurvePoint()
          MyPublicKey(curve, point.getX, point.getY)
        case _ => ???
      }
    }
  }
}
