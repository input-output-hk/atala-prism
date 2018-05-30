package io.iohk.cef.db

//import java.net.{InetAddress, InetSocketAddress}
//
//import akka.util.ByteString
//import io.iohk.cef.network.{Capabilities, Node}
import io.iohk.cef.test.TestClock
import org.scalatest.MustMatchers

class KnownNodeStorageImplSpec extends AutoRollbackSpec with MustMatchers {

  behavior of "KnownNodeStorageImpl"

//  it should "start with an empty set" in { implicit session =>
//    val clock = TestClock()
//    val storage = new KnownNodeStorageImpl(clock, 'test)
//    storage.getAll().size == 0
//  }
//
//  it should "insert a new node" in { implicit session =>
//    val clock = TestClock()
//    val storage = new KnownNodeStorageImpl(clock, 'test)
//    val addr = new InetSocketAddress(InetAddress.getByAddress(Array(1,2,3,4)),23)
//    val node = Node(ByteString("1"), addr, addr, Capabilities(1))
//    storage.insert(node)
//    storage.getAll() mustBe Set(node)
//  }
}
