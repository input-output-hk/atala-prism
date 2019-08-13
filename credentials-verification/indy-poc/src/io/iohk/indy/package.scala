package io.iohk

import java.nio.file.{Path, Paths}

import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet

import scala.util.Try

package object indy {
  private[indy] trait HasMain {
    def main(args: Array[String]): Unit
  }

  def getResourcePath(resource: String): Path = {
    val uri = this.getClass.getResource(resource).toURI
    Paths.get(uri).toAbsolutePath
  }

  val GENESIS_TXN: Path = getResourcePath("/docker_pool_transactions_genesis.txn")

  val DEFAULT_POOL_CONFIG_JSON: String = {
    s"""
       |{
       |  "genesis_txn":  "$GENESIS_TXN"
       |}
       |""".stripMargin
  }

  val DEFAULT_PROTOCOL_VERSION: Int = 2

  val DEFAULT_WALLET_NAME = "myWallet"
  val DEFAULT_POOL_NAME = "pool"

  val DEFAULT_WALLET_CONFIG_JSON = s"""{ "id": "$DEFAULT_WALLET_NAME" }"""
  val DEFAULT_WALLET_CREDENTIALS_JSON = """ { "key": "wallet_key" }"""

  val STEWARD_SEED = "000000000000000000000000Steward1"

  def prepareCleanEnvironment() = {
    Try {
      // ensure the ledger config doesn't exists
      Pool.deletePoolLedgerConfig(DEFAULT_POOL_NAME).get
    }

    Try {
      // ensure the wallet doesn't exists
      Wallet.deleteWallet(DEFAULT_WALLET_CONFIG_JSON, DEFAULT_WALLET_CREDENTIALS_JSON).get
    }

    val _ = Pool.setProtocolVersion(2).get()
  }
}
