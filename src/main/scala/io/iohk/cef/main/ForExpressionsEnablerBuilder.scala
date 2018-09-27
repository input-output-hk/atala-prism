package io.iohk.cef.main

import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

trait ForExpressionsEnablerBuilder[F[_]] {
  val forExpressionsEnabler: ForExpressionsEnabler[F]
}
