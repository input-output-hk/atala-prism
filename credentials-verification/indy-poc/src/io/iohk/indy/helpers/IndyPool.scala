package io.iohk.indy.helpers

import java.nio.file.Path

import io.iohk.indy.models._
import io.iohk.indy.{DEFAULT_PROTOCOL_VERSION, poolConfigJson}
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.pool.Pool

import scala.util.Try

class IndyPool(pool: Pool) {
  def submitRequest(singedRequestJson: String): JsonString = {
    val result = Ledger.submitRequest(pool, singedRequestJson).get
    JsonString(result)
  }

  def close() = pool.close()
}

object IndyPool {
  def apply(poolName: String, genesisTxn: Path): IndyPool = {
    val poolConfig = poolConfigJson(genesisTxn)
    Pool.setProtocolVersion(DEFAULT_PROTOCOL_VERSION).get()

    Try {
      // ensure the ledger config doesn't exists
      Pool.deletePoolLedgerConfig(poolName).get
    }
    Pool.createPoolLedgerConfig(poolName, poolConfig).get
    val pool = Pool.openPoolLedger(poolName, "{}").get
    new IndyPool(pool)
  }
}
