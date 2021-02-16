package io.iohk.atala.cvp.webextension.background.wallet.models

import io.iohk.atala.cvp.webextension.common.models.{CredentialSubject, PendingRequest}

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

  def list: List[PendingRequest] = map.values.map(_._1).toList

  def get(requestId: RequestId): Option[Value] = map.get(requestId)

  def -(requesId: RequestId): PendingRequestsQueue = {
    val newMap = map - requesId
    copy(map = newMap)
  }

  def issueCredential(
      origin: String,
      sessionID: String,
      subject: CredentialSubject
  ): (PendingRequestsQueue, Promise[String]) = {
    val promise = Promise[String]()
    val request: PendingRequest = PendingRequest.IssueCredential(totalRequests, origin, sessionID, subject)
    val item = (totalRequests, (request, promise))
    val newState = copy(
      totalRequests = totalRequests + 1,
      map = map + item
    )

    (newState, promise)
  }
}

object PendingRequestsQueue {
  type RequestId = Int
  type Value = (PendingRequest, Promise[String])

  def empty: PendingRequestsQueue = PendingRequestsQueue(0, Map.empty)
}
