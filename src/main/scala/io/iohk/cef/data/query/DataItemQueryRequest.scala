package io.iohk.cef.data.query
import io.iohk.cef.network.NodeId

case class DataItemQueryRequest(id: String, query: DataItemQuery, replyTo: NodeId)
