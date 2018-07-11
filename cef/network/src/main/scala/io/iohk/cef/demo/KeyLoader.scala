package io.iohk.cef.demo

import java.io.{File, PrintWriter}
import java.security.SecureRandom

import io.iohk.cef.crypto.{generateKeyPair, keyPairFromPrvKey, keyPairToByteArrays}
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.util.encoders.Hex

import scala.io.Source

object KeyLoader {

  def loadAsymmetricCipherKeyPair(filePath: String, secureRandom: SecureRandom): AsymmetricCipherKeyPair = {
    val file = new File(filePath)
    if(!file.exists()){
      val keysValuePair = generateKeyPair(secureRandom)

      //Write keys to file
      val (priv, _) = keyPairToByteArrays(keysValuePair)
      require(file.getParentFile.exists() || file.getParentFile.mkdirs(), "Key's file parent directory creation failed")
      val writer = new PrintWriter(filePath)
      try {
        writer.write(Hex.toHexString(priv))
      } finally {
        writer.close()
      }

      keysValuePair
    } else {
      val reader = Source.fromFile(filePath)
      try {
        val privHex = reader.mkString
        keyPairFromPrvKey(Hex.decode(privHex))
      } finally {
        reader.close()
      }
    }
  }
}
