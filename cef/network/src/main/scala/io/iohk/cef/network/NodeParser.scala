package io.iohk.cef.network

import java.net.{InetAddress, URI}

import akka.util.ByteString
import com.typesafe.config.Config
import io.iohk.cef.utils.Logger

import scala.util.{Failure, Success, Try}

object NodeParser extends Logger {
  val NodeScheme = "enode"
  val UdpScheme = "udp"
  val NodeIdSize = 128


  type Error = String

  private def validateTcpAddress(uri: URI): Either[Error, URI] = {
    Try(InetAddress.getByName(uri.getHost) -> uri.getPort) match {
      case Success(tcpAddress) if tcpAddress._2 != -1 => Right(uri)
      case Success(_)  => Left(s"No defined port for uri $uri")
      case Failure(_)  => Left(s"Error parsing ip address for $uri")
    }
  }

  private def validateScheme(uri: URI, expectedScheme: String): Either[Error, URI] = {
    val scheme = Option(uri.getScheme).toRight(s"No defined scheme for uri $uri")

    scheme.flatMap{ scheme =>
      Either.cond(uri.getScheme == expectedScheme, uri, s"Invalid node scheme $scheme, it should be $expectedScheme")
    }
  }

  private def validateNodeId(uri: URI): Either[Error, URI] = {
    val nodeId = Try(ByteString(uri.getUserInfo)) match {
      case Success(id) => Right(id)
      case Failure(_) => Left(s"Malformed nodeId for URI ${uri.toString}")
    }

    nodeId.flatMap(nodeId =>
      Either.cond(nodeId.size == NodeIdSize, uri, s"Invalid node id $nodeId size, it should be $NodeIdSize")
    )
  }

  private def validateUri(uriString: String): Either[Set[Error], URI] = {
    Try(new URI(uriString)) match {
      case Success(nUri) => Right(nUri)
      case Failure(ex) => Left(Set(s"Malformed URI for node $uriString"))
    }
  }

  private def validateNodeUri(node: String): Either[Set[Error], URI] = {
    import io.iohk.cef.utils.ValidationUtils._

    val uri = validateUri(node)
    uri match {
      case Left(error) => Left(error)
      case Right(nUri)  =>
        val valScheme = validateScheme(nUri, NodeScheme)
        val valNodeId = validateNodeId(nUri)
        val valTcpAddress = validateTcpAddress(nUri)
        combineValidations(nUri, valScheme, valNodeId, valTcpAddress)
    }
  }

  private def validateUdpUri(udp: String): Either[Set[Error], URI] = {
    import io.iohk.cef.utils.ValidationUtils._

    val uri = validateUri(udp)
    uri match {
      case Left(error) => Left(error)
      case Right(nUri)  =>
        val valScheme = validateScheme(nUri, UdpScheme)
        val valTcpAddress = validateTcpAddress(nUri)
        combineValidations(nUri, valScheme, valTcpAddress)
    }
  }

  /**
    * Parse a node string, for it to be valid it should have the format:
    * "enode://[128 char (64bytes) hex string]@[IPv4 address | '['IPv6 address']' ]:[port]"
    *
    * @param nodeConfig to be parsed
    * @return the parsed node, or the errors detected during parsing
    */
  def parseNode(nodeConfig: Config): Either[Set[Error], Node] = {
    val discoveryUri = nodeConfig.getString("discoveryUri")
    val p2pUri = nodeConfig.getString("p2pUri")
    val validationP2p = validateNodeUri(p2pUri)
    val validationDisc = validateUdpUri(discoveryUri)
    val errorSet = validationP2p.left.getOrElse(validationDisc.left.getOrElse(Set()))
    for {
      p2pUri <- validationP2p
      discUri <- validationDisc
      node <- Node.fromUri(p2pUri, discUri, nodeConfig.getString("capabilities")) match {
        case Success(node) => Right(node)
        case Failure(ex) => Left(errorSet + ex.getMessage)
      }
    } yield node
  }

  /**
    * Parses a set of nodes, logging the invalid ones and returning the valid ones
    *
    * @param unParsedNodes, nodes to be parsed
    * @return set of parsed and valid nodes
    */
  def parseNodes(unParsedNodes: Set[Config]): Set[Node] = unParsedNodes.foldLeft[Set[Node]](Set.empty) {
    case (parsedNodes, nodeConfig) =>
      val maybeNode = NodeParser.parseNode(nodeConfig)
      maybeNode match {
        case Right(node) => parsedNodes + node
        case Left(errors) =>
          log.warn(s"Unable to parse node: $nodeConfig due to: $errors")
          parsedNodes
      }
  }
}
