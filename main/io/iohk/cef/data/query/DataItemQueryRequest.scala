package io.iohk.cef.data.query
import io.iohk.network.NodeId

case class DataItemQueryRequest(id: String, query: DataItemQuery, replyTo: NodeId)
