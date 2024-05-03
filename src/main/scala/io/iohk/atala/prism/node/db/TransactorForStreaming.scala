package io.iohk.atala.prism.node.db

import cats.effect.IO
import doobie.Transactor

//This class is just to help tag a Transactor for the specific use case of streaming
class TransactorForStreaming(val transactor: Transactor[IO]) extends AnyVal
