package io.iohk.atala.cvp.webextension.activetab.models

import io.circe.generic.auto._
import io.circe.parser.parse

import scala.util.Try

/**
  * Internal typed-message to request the extension isolated context to perform an operation.
  */
private[activetab] sealed trait Command extends Product with Serializable

private[activetab] object Command {

  final case class GetWalletStatus() extends Command

  final case class GetUserDetails() extends Command

  def decode(string: String): Try[Command] = {
    parse(string).toTry
      .flatMap(_.as[Command].toTry)
  }
}
