package io.iohk.cef.ledger

case class LedgerStateUpdateActions[S](insert: Set[(String, S)],
                                       delete: Set[(String, S)],
                                       update: Set[(String, S)])
