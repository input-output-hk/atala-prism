package io.iohk.cef.network

import java.net.{InetAddress, URI}

import akka.util.ByteString
import io.iohk.cef.utils.Logger

import scala.util.{Failure, Success, Try}

object NodeParser extends Logger {
  val NodeScheme = "enode"
  val NodeIdSize = 5


  type Error = String

  private def validateTcpAddress(uri: URI): Either[Error, URI] = {
    Try(InetAddress.getByName(uri.getHost) -> uri.getPort) match {
      case Success(tcpAddress) if tcpAddress._2 != -1 => Right(uri)
      case Success(_)  => Left(s"No defined port for uri $uri")
      case Failure(_)  => Left(s"Error parsing ip address for $uri")
    }
  }

  private def validateScheme(uri: URI): Either[Error, URI] = {
    val scheme = Option(uri.getScheme).toRight(s"No defined scheme for uri $uri")

    scheme.flatMap{ scheme =>
      Either.cond(uri.getScheme == NodeScheme, uri, s"Invalid node scheme $scheme, it should be $NodeScheme")
    }
  }

  private def validateNodeId(uri: URI): Either[Error, URI] = {
    val nodeId = Try(ByteString(uri.getUserInfo)) match {
      case Success(id) => Right(id)
      case Failure(_) => Left(s"Malformed nodeId for URI ${uri.toString}")
    }

    nodeId.flatMap(nodeId =>
      Either.cond(nodeId.size == NodeIdSize, uri, s"Invalid node scheme $nodeId size, it should be $NodeScheme")
    )
  }

  private def validateUri(uriString: String): Either[Error, URI] = {
    Try(new URI(uriString)) match {
      case Success(nUri) => Right(nUri)
      case Failure(ex) => Left(s"Malformed URI for node $uriString")
    }
  }

  private def validateNodeUri(node: String): Either[Set[Error], URI] = {
    import io.iohk.cef.utils.ValidationUtils._

    val uri = validateUri(node)
    uri match {
      case Left(error) => Left(Set(error))
      case Right(nUri)  =>
        val valScheme = validateScheme(nUri)
        val valNodeId = validateNodeId(nUri)
        val valTcpAddress = validateTcpAddress(nUri)
        combineValidations(nUri, valScheme, valNodeId, valTcpAddress)
    }
  }

  /**
    * Parse a node string, for it to be valid it should have the format:
    * "enode://[128 char (64bytes) hex string]@[IPv4 address | '['IPv6 address']' ]:[port]"
    *
    * @param node to be parsed
    * @return the parsed node, or the errors detected during parsing
    */
  def parseNode(node:String): Either[Set[Error], Node] = {
    val validation = validateNodeUri(node)
    val errorSet = validation.left.getOrElse(Set())
    validation.flatMap(uri => Node.fromUri(uri) match {
      case Success(node) => Right(node)
      case Failure(ex) => Left(errorSet +  ex.getMessage)
    })
  }

  /**
    * Parses a set of nodes, logging the invalid ones and returning the valid ones
    *
    * @param unParsedNodes, nodes to be parsed
    * @return set of parsed and valid nodes
    */
  def parseNodes(unParsedNodes: Set[String]): Set[Node] = unParsedNodes.foldLeft[Set[Node]](Set.empty) {
    case (parsedNodes, nodeString) =>
      val maybeNode = NodeParser.parseNode(nodeString)
      maybeNode match {
        case Right(node) => parsedNodes + node
        case Left(errors) =>
          log.warn(s"Unable to parse node: $nodeString due to: $errors")
          parsedNodes
      }
  }
}
