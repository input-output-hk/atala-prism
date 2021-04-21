package io.iohk.atala.cvp.webextension.background.wallet.models

import io.iohk.atala.cvp.webextension.common.models.PendingRequest
import io.iohk.atala.cvp.webextension.common.models.PendingRequest.{
  IssueCredential,
  IssueCredentialWithId,
  RevokeCredential,
  RevokeCredentialWithId
}

import scala.concurrent.Promise

/**
  * The state for requests requiring to be reviewed
  *
  * @param totalRequests the total number of requests that have got into the wallet
  * @param map the pending requests, indexed by request id, each request has the promise
  *            used to respond to after the request is handled by the user.
  */
case class PendingRequestsQueue(
    totalRequests: Int,
    map: Map[PendingRequestsQueue.RequestId, PendingRequestsQueue.Value]
) {
  import PendingRequestsQueue._

  def size: Int = map.size

  def list: List[PendingRequest.WithId] = map.values.map(_._1).toList

  def removeAll(requestIds: List[RequestId]): PendingRequestsQueue = {
    val newMap = map.removedAll(requestIds)
    copy(map = newMap)
  }

  def issuanceCredentialRequests: List[IssueCredentialWithId] = {
    list.collect {
      case PendingRequest.WithId(id, r @ IssueCredential(_)) => IssueCredentialWithId(id, r)
    }
  }

  def revocationRequests: List[RevokeCredentialWithId] = {
    list.collect {
      case PendingRequest.WithId(id, r @ RevokeCredential(_, _, _, _)) => RevokeCredentialWithId(id, r)
    }
  }

  def get(requestId: RequestId): Option[Value] = map.get(requestId)

  def -(requesId: RequestId): PendingRequestsQueue = {
    val newMap = map - requesId
    copy(map = newMap)
  }

  def enqueue(request: PendingRequest): (PendingRequestsQueue, Promise[String]) = {
    val promise = Promise[String]()
    val item = (totalRequests, (PendingRequest.WithId(totalRequests, request), promise))
    val newState = copy(
      totalRequests = totalRequests + 1,
      map = map + item
    )

    (newState, promise)
  }
}

object PendingRequestsQueue {
  type RequestId = Int
  type Value = (PendingRequest.WithId, Promise[String])

  def empty: PendingRequestsQueue = PendingRequestsQueue(0, Map.empty)
}
