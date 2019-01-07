package io.iohk.cef.data.query
import io.iohk.cef.network.NodeId

case class QueryRequest(id: String, query: Query, replyTo: NodeId)
