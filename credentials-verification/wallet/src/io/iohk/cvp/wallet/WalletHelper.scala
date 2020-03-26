package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import io.iohk.cvp.crypto.ECKeys
import io.iohk.prism.protos.wallet_models

object WalletHelper {

  def toPrivateKey(proto: wallet_models.ECPrivateKey): PrivateKey = {
    proto.d
      .map(_.value)
      .map(BigInt.apply)
      .map(ECKeys.toPrivateKey)
      .getOrElse(fatalWalletCorrupted)
  }

  def toPublicKey(proto: wallet_models.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(fatalWalletCorrupted)
  }

  private def fatalWalletCorrupted = {
    throw new RuntimeException("The wallet is likely corrupted, you'll need to repair it or delete it manually")
  }

  def toPublicKeyProto(key: PublicKey): wallet_models.ECPublicKey = {
    val point = ECKeys.getECPoint(key)
    wallet_models
      .ECPublicKey()
      .withX(wallet_models.BigInteger(point.getAffineX.toString))
      .withY(wallet_models.BigInteger(point.getAffineY.toString))
  }

  def toPrivateKeyProto(key: PrivateKey): wallet_models.ECPrivateKey = {
    wallet_models.ECPrivateKey().withD(wallet_models.BigInteger(ECKeys.getD(key).toString))
  }
}
