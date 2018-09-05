package io.iohk.cef.consensus.raft.node

import io.iohk.cef.consensus.raft.node.FutureOps.sequenceForgiving
import io.iohk.cef.consensus.raft._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

private[raft] sealed trait NodeRole[Command] {
  val stateCode: StateCode
  def appendEntries(
      rc: RaftState[Command],
      entriesToAppend: EntriesToAppend[Command]): (RaftState[Command], AppendEntriesResult)
  def clientAppendEntries(
      rc: RaftState[Command],
      entries: Seq[Command]): Future[(RaftState[Command], Either[Redirect[Command], Unit])]
}

case object Follower extends StateCode
case object Candidate extends StateCode
case object Leader extends StateCode

private[raft] class Follower[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
    extends NodeRole[Command] {

  override def appendEntries(
      rc: RaftState[Command],
      entriesToAppend: EntriesToAppend[Command]): (RaftState[Command], AppendEntriesResult) = {
    applyAppendEntriesRules1To5(rc, entriesToAppend)
  }

  // AppendEntries RPC receiver implementation (figure 2), rules 1-5
  private def applyAppendEntriesRules1To5(
      rc: RaftState[Command],
      entriesToAppend: EntriesToAppend[Command]): (RaftState[Command], AppendEntriesResult) = {

    val log = rc.log
    val (currentTerm, _) = rc.persistentState
    if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {
      (rc, AppendEntriesResult(term = currentTerm, success = false))
    } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {
      (rc, AppendEntriesResult(term = currentTerm, success = false))
    } else {
      val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
      if (conflicts != 0) {
        val rc2 = rc.copy(leaderId = entriesToAppend.leaderId, deletes = conflicts)
        (rc2, AppendEntriesResult(term = currentTerm, success = false))
      } else {
        val additions: Seq[LogEntry[Command]] = appendEntriesAdditions(log, entriesToAppend)
        val rc2 = rc.copy(leaderId = entriesToAppend.leaderId, writes = rc.writes ++ additions)
        val rc3 = appendEntriesCommitIndexCheck(rc2, entriesToAppend.leaderCommitIndex, iLastNewEntry(additions))
        (rc3, AppendEntriesResult(term = currentTerm, success = true))
      }
    }
  }

  private def iLastNewEntry(additions: Seq[LogEntry[Command]]): Int =
    additions.lastOption.map(_.index).getOrElse(Int.MaxValue)

  // AppendEntries summary note #1 (Sec 5.1 consistency check)
  private def appendEntriesConsistencyCheck1(term: Int, currentTerm: Int): Boolean =
    term < currentTerm

  // AppendEntries summary note #2 (Sec 5.3 Consistency check)
  private def appendEntriesConsistencyCheck2(log: IndexedSeq[LogEntry[Command]], prevLogIndex: Int): Boolean = {
    @tailrec
    def loop(i: Int): Boolean = {
      if (i == -1)
        true
      else if (log(i).index == prevLogIndex)
        false
      else
        loop(i - 1)
    }
    if (prevLogIndex == -1) // need to handle the case of leader with empty log
      false
    else
      loop(log.size - 1)
  }

  // AppendEntries summary note #3 (Sec 5.3 deleting inconsistent log entries)
  private def appendEntriesConflictSearch(
      log: IndexedSeq[LogEntry[Command]],
      entriesToAppend: EntriesToAppend[Command]): Int = {

    val logSz = log.size

    val minEntryIndex: Int = entriesToAppend.entries.headOption.map(head => head.index).getOrElse(logSz)

    @tailrec
    def loop(i: Int, iMin: Int): Int = { // reverse search for the minimum index of a conflicting entry

      if (i == -1)
        iMin
      else {
        val current: LogEntry[Command] = log(i)

        if (minEntryIndex > current.index) { // all entries to append have a higher index than current, terminate reverse search
          iMin
        } else {
          // same index but different terms check
          val maybeConflictingEntry: Option[LogEntry[Command]] =
            entriesToAppend.entries.find(entryToAppend =>
              current.index == entryToAppend.index && current.term != entryToAppend.term)

          if (maybeConflictingEntry.isDefined)
            loop(i - 1, i)
          else
            loop(i - 1, iMin)
        }
      }
    }

    val iMin = loop(logSz - 1, logSz)

    logSz - iMin
  }

  // AppendEntries summary note #4 (append new entries not already in the log)
  private def appendEntriesAdditions(
      log: IndexedSeq[LogEntry[Command]],
      entriesToAppend: EntriesToAppend[Command]): Seq[LogEntry[Command]] = {

    val logSz = log.size

    val minEntryIndex: Int = entriesToAppend.entries.headOption.map(head => head.index).getOrElse(logSz)

    // can assume the terms are consistent here.
    // find the entriesToAppend entry without a matching log entry.
    @tailrec
    def loop(i: Int, nDrop: Int): Int = {
      if (i == -1)
        nDrop
      else {
        val current = log(i)

        if (minEntryIndex > current.index)
          nDrop
        else {
          val maybeOverlappingEntry: Option[LogEntry[Command]] =
            entriesToAppend.entries.find(entryToAppend => current.index == entryToAppend.index)

          if (maybeOverlappingEntry.isDefined)
            loop(i - 1, nDrop + 1)
          else
            loop(i - 1, nDrop)

        }
      }
    }
    val nDrop = loop(logSz - 1, 0)
    entriesToAppend.entries.drop(nDrop)
  }

  // AppendEntries summary note #5 (if leaderCommit > commitIndex,
  // set commitIndex = min(leaderCommit, index of last new entry)
  private def appendEntriesCommitIndexCheck(
      rc: RaftState[Command],
      leaderCommitIndex: Int,
      iLastNewEntry: Int): RaftState[Command] = {
    val commitIndex = Math.min(leaderCommitIndex, iLastNewEntry)

    if (leaderCommitIndex > rc.commonVolatileState.commitIndex) {
      rc.copy(commonVolatileState = rc.commonVolatileState.copy(commitIndex = commitIndex))
    } else {
      rc
    }
  }
  override def clientAppendEntries(
      rc: RaftState[Command],
      entries: Seq[Command]): Future[(RaftState[Command], Either[Redirect[Command], Unit])] =
    Future((rc, Left(Redirect(raftNode.getLeaderRPC))))
  override val stateCode: StateCode = Follower
}

private[raft] class Candidate[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
    extends NodeRole[Command] {
  override def appendEntries(
      rc: RaftState[Command],
      entriesToAppend: EntriesToAppend[Command]): (RaftState[Command], AppendEntriesResult) = {
    // rules for servers, candidates
    // if append entries rpc received from new leader, convert to follower
    val prospectiveLeaderTerm = entriesToAppend.term
    val (currentTerm, _) = rc.persistentState
    if (prospectiveLeaderTerm >= currentTerm) {
      // the term will have been updated via rules for servers note 2.
      val rc2 = raftNode.nodeFSM(rc, LeaderDiscovered)
      (rc2, AppendEntriesResult(currentTerm, success = true))
    } else {
      (rc, AppendEntriesResult(currentTerm, success = false))
    }
  }
  override def clientAppendEntries(
      rc: RaftState[Command],
      entries: Seq[Command]): Future[(RaftState[Command], Either[Redirect[Command], Unit])] =
    Future((rc, Left(Redirect(raftNode.getLeaderRPC))))

  override val stateCode: StateCode = Candidate
}

private[raft] class Leader[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
    extends NodeRole[Command] {

  override val stateCode: StateCode = Leader

  override def appendEntries(
      rc: RaftState[Command],
      entriesToAppend: EntriesToAppend[Command]): (RaftState[Command], AppendEntriesResult) = {
    val prospectiveLeaderTerm = entriesToAppend.term
    val (currentTerm, _) = rc.persistentState
    if (prospectiveLeaderTerm >= currentTerm) {
      // the term will have been updated via rules for servers note 2.
      val rc2 = raftNode.nodeFSM(rc, NodeWithHigherTermDiscovered)
      (rc2, AppendEntriesResult(currentTerm, success = true))
    } else {
      (rc, AppendEntriesResult(currentTerm, success = false))
    }
  }

  override def clientAppendEntries(
      rc: RaftState[Command],
      entries: Seq[Command]): Future[(RaftState[Command], Either[Redirect[Command], Unit])] = {
    val log = rc.log
    val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(log)
    val (currentTerm, _) = rc.persistentState

    val (_, entriesToAppend) = entries.foldLeft((lastLogIndex, Vector[LogEntry[Command]]()))((acc, nextCommand) => {
      val (logIndex, entriesToAppend) = acc
      val nextLogIndex = logIndex + 1
      val nextEntry = LogEntry[Command](nextCommand, currentTerm, nextLogIndex)
      (nextLogIndex, entriesToAppend :+ nextEntry)
    })

    val rc2 = rc.copy(writes = entriesToAppend)

    sendAppendEntries(rc2).map(rcf => (rcf, Right(())))
  }

  private def sendAppendEntries(rc: RaftState[Command]): Future[RaftState[Command]] = {
    val nextIndexes = rc.leaderVolatileState.nextIndex
    val (currentTerm, _) = rc.persistentState
    val commitIndex = rc.commonVolatileState.commitIndex
    val theLog = rc.log
    val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(theLog)

    val callFs: Seq[Future[(Int, Int)]] = raftNode.clusterMembers.indices.map(i =>
      sendAppendEntry(rc, nextIndexes(i), lastLogIndex, raftNode.clusterMembers(i)))

    val indicesF: Future[(Seq[Int], Seq[Int])] =
      sequenceForgiving(callFs).map((indices: Seq[(Int, Int)]) => indices.unzip)

    for {
      (nextIndex, matchIndex) <- indicesF
    } yield {
      // Leaders, final note (#4)
      val maybeN = findN(theLog, currentTerm, commitIndex, matchIndex)

      val nextCommonState =
        maybeN.fold(rc.commonVolatileState)(n => rc.commonVolatileState.copy(commitIndex = n))

      val nextLeaderState = LeaderVolatileState(nextIndex, matchIndex)

      rc.copy(commonVolatileState = nextCommonState, leaderVolatileState = nextLeaderState)
    }
  }

  private def sendAppendEntry(
      rc: RaftState[Command],
      nextIndex: Int,
      lastLogIndex: Int,
      memberRPC: RPC[Command]): Future[(Int, Int)] = {
    val commitIndex = rc.commonVolatileState.commitIndex

    val (currentTerm, _) = rc.persistentState
    val appendResultF =
      appendEntryForNode(nextIndex, commitIndex, rc.log, currentTerm, lastLogIndex, memberRPC, getHeartbeat(rc))

    appendResultF.flatMap(appendResult => {
      if (appendResult.success) {
        // Leaders, If successful, update nextIndex and matchIndex for follower.
        Future((lastLogIndex + 1, lastLogIndex))
      } else {
        // Leaders, if fails because of log inconsistency, retry.
        sendAppendEntry(rc, nextIndex - 1, lastLogIndex, memberRPC)
      }
    })
  }

  private def appendEntryForNode(
      nextIndex: Int,
      commitIndex: Int,
      theLog: IndexedSeq[LogEntry[Command]],
      currentTerm: Int,
      lastLogIndex: Int,
      peerRpc: RPC[Command],
      heartbeat: EntriesToAppend[Command]): Future[AppendEntriesResult] = {

    if (lastLogIndex >= nextIndex) { // Rules for servers, leaders, note #3.
      val entries = theLog.slice(nextIndex, theLog.size) // NB: will break after impl of log compaction
      val (prevLogIndex, prevLogTerm) =
        if (nextIndex == 0)
          (-1, -1)
        else {
          val previousEntry = theLog(nextIndex - 1)
          (previousEntry.index, previousEntry.term)
        }
      val entriesToAppend =
        EntriesToAppend(currentTerm, raftNode.nodeId, prevLogIndex, prevLogTerm, entries, commitIndex)
      peerRpc.appendEntries(entriesToAppend)
    } else {
      peerRpc.appendEntries(heartbeat)
    }
  }

  private def findN(
      log: IndexedSeq[LogEntry[Command]],
      currentTerm: Int,
      commitIndex: Int,
      matchIndex: Seq[Int]): Option[Int] = {
    @tailrec
    def loop(successN: List[Int], tryN: Int): List[Int] = {
      if (log.size > tryN) {
        val matchingTermN = log(tryN).term == currentTerm

        val majorityN = (matchIndex.count(m => m >= tryN) + 1) > (raftNode.clusterMembers.size + 1) / 2

        if (matchingTermN && majorityN)
          loop(tryN :: successN, tryN + 1)
        else
          loop(successN, tryN + 1)
      } else {
        successN
      }
    }

    val ns = loop(List(), commitIndex + 1)
    if (ns.isEmpty)
      None
    else
      Some(ns.max)
  }
  private def getHeartbeat(rc: RaftState[Command]): EntriesToAppend[Command] = {
    val (currentTerm, _) = rc.persistentState
    val (prevLogIndex, prevLogTerm) = raftNode.lastLogIndexAndTerm(rc.log)
    val commitIndex = rc.commonVolatileState.commitIndex
    EntriesToAppend[Command](currentTerm, raftNode.nodeId, prevLogIndex, prevLogTerm, Seq(), commitIndex)
  }
}
