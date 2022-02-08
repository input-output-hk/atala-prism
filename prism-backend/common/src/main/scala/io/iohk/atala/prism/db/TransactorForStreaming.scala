package io.iohk.atala.prism.db

import cats.effect.IO
import doobie.Transactor

//This class is just to help tag a Transactor for the specific use case of streaming
final class TransactorForStreaming(private[db] val value: Transactor[IO]) extends AnyVal {
  def transactor = value
}
