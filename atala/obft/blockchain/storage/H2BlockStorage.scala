package atala.obft.blockchain.storage

import java.nio.ByteBuffer

import atala.clock.TimeSlot
import atala.obft.blockchain.models._
import cats.effect._
import doobie._
import doobie.implicits._
import io.iohk.decco.BufferInstantiator.global.HeapByteBuffer
import io.iohk.decco.Codec
import io.iohk.multicrypto._

import scala.concurrent.ExecutionContext

class H2BlockStorage[Tx](xa: Transactor[IO])(implicit txCodec: Codec[List[Tx]]) extends BlockStorage[Tx] {

  import H2BlockStorage._

  createSchema()

  private def createSchema(): Unit = {
    createBlocks.update.run
      .transact(xa)
      .unsafeRunSync()
  }

  override def get(hash: Hash): Option[Block[Tx]] = {
    val program =
      sql"""
           |SELECT hash, delta, previous_hash, signature, time_slot, time_slot_signature
           |FROM blocks
           |WHERE hash = ${hash.toCompactString()}
       """.stripMargin.query[Block[Tx]].option

    program.transact(xa).unsafeRunSync()
  }

  override def put(hash: Hash, block: Block[Tx]): Unit = {
    val hashString = hash.toCompactString()
    val previousHash = block.body.previousHash.toCompactString()
    val signature = block.signature.toCompactString()
    val timeSlot = block.body.timeSlot.index
    val timeSlotSignature = block.body.timeSlotSignature.toCompactString()

    val delta = txCodec.encode(block.body.delta).array()
    val program = sql"""
                       |INSERT INTO blocks (hash, delta, previous_hash, signature, time_slot, time_slot_signature)
                       |VALUES ($hashString, $delta, $previousHash, $signature, $timeSlot, $timeSlotSignature)
       """.stripMargin.update

    program.run.transact(xa).unsafeRunSync()
  }

  override def remove(hash: Hash): Unit = {
    val program =
      sql"""
           |DELETE blocks
           |WHERE hash = ${hash.toCompactString()}
           """.stripMargin.update

    program.run.transact(xa).unsafeRunSync()
  }
}

object H2BlockStorage {

  def apply[Tx](database: String)(implicit txCodec: Codec[List[Tx]]): H2BlockStorage[Tx] = {
    require(database.trim.nonEmpty, "The database name should not be empty")

    // A transactor that gets connections from java.sql.DriverManager and excutes blocking operations
    // on an unbounded pool of daemon threads. See the chapter on connection handling for more info.
    val xa = Transactor.fromDriverManager[IO](
      "org.h2.Driver",
      s"jdbc:h2:./$database",
      "sa",
      ""
    )

    new H2BlockStorage[Tx](xa)
  }

  // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
  // is where nonblocking operations will be executed.
  private implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private implicit val hashGet: Get[Hash] = Get[String].tmap { string =>
    Hash
      .parseFrom(string)
      .right
      .getOrElse(throw new RuntimeException("Corrupted hash"))
  }

  private implicit val signatureGet: Get[Signature] = Get[String].tmap { string =>
    Signature
      .parseFrom(string)
      .right
      .getOrElse(throw new RuntimeException("Corrupted signature"))
  }

  private implicit val timeSlotGet: Get[TimeSlot] = Get[Int].tmap(TimeSlot.apply)

  private implicit def deltaGet[Tx](implicit txCodec: Codec[List[Tx]]): Read[List[Tx]] = Read[Array[Byte]].map {
    bytes =>
      txCodec
        .decode(ByteBuffer.wrap(bytes))
        .right
        .getOrElse(throw new RuntimeException("Corrupted delta"))
  }

  private implicit def blockRead[Tx](implicit txCodec: Codec[List[Tx]]): Read[Block[Tx]] =
    Read[(Hash, List[Tx], Hash, Signature, TimeSlot, Signature)]
      .map {
        case (_, delta, previousHash, signature, timeSlot, timeSlotSignature) =>
          Block(
            BlockBody(previousHash, delta, timeSlot, timeSlotSignature),
            signature
          )
      }

  private val createBlocks =
    sql"""
         |CREATE TABLE IF NOT EXISTS blocks (
         |  hash VARCHAR NOT NULL PRIMARY KEY,
         |  delta BINARY NOT NULL,
         |  previous_hash VARCHAR NOT NULL,
         |  signature VARCHAR NOT NULL,
         |  time_slot INT NOT NULL,
         |  time_slot_signature VARCHAR NOT NULL
         |);
         """.stripMargin
}
