package io.iohk.cef.db

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, Node}
import io.iohk.cef.test.TestClock
import org.scalatest.MustMatchers

import scala.concurrent.duration.FiniteDuration

class KnownNodeStorageImplSpec extends AutoRollbackSpec with MustMatchers {

  behavior of "KnownNodeStorageImpl"

  it should "start with an empty set" in { f =>
    val clock = TestClock()
    val storage = new KnownNodeStorageImpl(clock)
    storage.getAll().size == 0
  }

  it should "insert a new node" in { f =>
    val clock = TestClock()
    val storage = new KnownNodeStorageImpl(clock)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = Node(ByteString("1"), addr, addr, Capabilities(1))
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, clock.instant(), clock.instant()))
  }

  it should "update last seen of a node" in { f =>
    val clock = TestClock()
    val storage = new KnownNodeStorageImpl(clock)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = Node(ByteString("1"), addr, addr, Capabilities(1))
    val now = clock.instant()
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
    clock.tick
    val after = clock.instant()
    now must not be after
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, after))
  }

  it should "remove a node" in { f =>
    val clock = TestClock()
    val storage = new KnownNodeStorageImpl(clock)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = Node(ByteString("1"), addr, addr, Capabilities(1))
    val now = clock.instant()
    storage.insert(node)
    storage.getAll() mustBe Set(KnownNode(node, now, now))
    storage.remove(node)
    storage.getAll() mustBe Set()
  }

  it should "blacklist a node" in { f =>
    val clock = TestClock()
    val storage = new KnownNodeStorageImpl(clock)
    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
    val node = Node(ByteString("1"), addr, addr, Capabilities(1))
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
