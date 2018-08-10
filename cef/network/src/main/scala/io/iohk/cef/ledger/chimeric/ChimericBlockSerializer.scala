package io.iohk.cef.ledger.chimeric

import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable

object ChimericBlockSerializer {

  implicit val serializable: ByteStringSerializable[ChimericLedgerBlock] =
    new ByteStringSerializable[ChimericLedgerBlock] {

      override def deserialize(bytes: ByteString): ChimericLedgerBlock = ???

      override def serialize(t: ChimericLedgerBlock): ByteString = ???
    }

}
