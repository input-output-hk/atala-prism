package io.iohk.cef.db

import java.net.{InetAddress, InetSocketAddress}
import java.time.Clock
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, NodeInfo}
import io.iohk.cef.telemetery.InMemoryTelemetry
import io.iohk.cef.test.TestClock
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.DBSession
import scalikejdbc.scalatest.AutoRollback

import scala.concurrent.duration.FiniteDuration

trait KnownNodeStorageImplDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers {

  def createKnownNodeStorage(clock: Clock, session: DBSession): KnownNodeStorage =
    new KnownNodeStorageImpl(clock) with InMemoryTelemetry {
    override def inTx[T](block: DBSession => T): T = block(session)
  }

  behavior of "KnownNodeStorageImpl"

  it should "start with an empty set" in { session =>
    val clock = TestClock()
    val storage = createKnownNodeStorage(clock, session)
    storage.getAll().size == 0
  }

  it should "insert a new node" in { session =>
    val clock = TestClock()
    val storage = createKnownNodeStorage(clock, session)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = NodeInfo(ByteString("1"), addr, addr, Capabilities(1))
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, clock.instant(), clock.instant()))
  }

  it should "update last seen of a node" in { session =>
    val clock = TestClock()
    val storage = createKnownNodeStorage(clock, session)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = NodeInfo(ByteString("1"), addr, addr, Capabilities(1))
    val now = clock.instant()
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
    clock.tick
    val after = clock.instant()
    now must not be after
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, after))
  }

  it should "remove a node" in { session =>
    val clock = TestClock()
    val storage = createKnownNodeStorage(clock, session)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = NodeInfo(ByteString("1"), addr, addr, Capabilities(1))
    val now = clock.instant()
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
    storage.remove(node)
    storage.getAll() mustBe Set()
  }

  it should "blacklist a node" in { session =>
    val clock = TestClock()
    val storage = createKnownNodeStorage(clock, session)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = NodeInfo(ByteString("1"), addr, addr, Capabilities(1))
    val now = clock.instant()
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
    val duration = FiniteDuration(10, TimeUnit.SECONDS)
    storage.blacklist(node, duration)
    storage.getAll() mustBe Set()
    clock.tick(duration)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
  }
}
