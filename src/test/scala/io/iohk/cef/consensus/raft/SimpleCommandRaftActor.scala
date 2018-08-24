package io.iohk.cef.consensus.raft


class SimpleCommandRaftActor extends RaftActor
case class AppendTestRPC(word: String)
