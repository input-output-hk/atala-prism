package io.iohk.atala.cvp.webextension.activetab.models

import io.circe.generic.auto._
import io.circe.parser.parse

import scala.util.Try

/**
  * Internal typed-message used by the isolated extension context to reply to an operation
  */
private[activetab] sealed trait Event extends Product with Serializable

private[activetab] object Event {

  // TODO: Find a better way, possible returning something like Either[CommandRejected, Event]
  final case class CommandRejected(reason: String) extends Event
  final case class GotWalletStatus(status: String) extends Event

  def decode(string: String): Try[Event] = {
    parse(string).toTry
      .flatMap(_.as[Event].toTry)
  }
}
