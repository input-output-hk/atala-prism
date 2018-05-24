package io.iohk.cef.utils

import java.net.InetAddress

import javax.net.ServerSocketFactory

import scala.util.Random

object PortFinder {
  private def isAvailable(port: Int): Boolean = {
    try {
      ServerSocketFactory.getDefault.createServerSocket(
        port, 1, InetAddress.getByName("localhost")).close()
      true
    }
    catch {
      case _: Throwable => false
    }
  }

  private def randomPort = 1024 + Random.nextInt(65535 - 1024 + 1)

  def aPort: Int = {
    val port = randomPort
    if (isAvailable(port))
      port
    else
      aPort
  }
}
