package io.iohk.atala.prism.auth.errors

import io.grpc.Status
import tofu.logging.{DictLoggable, LogRenderer}

object grpcStatusLoggable {
  implicit val statusLoggable = new DictLoggable[Status] {
    override def fields[I, V, R, S](a: Status, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("grpc_status", a.toString, i)
    }

    override def logShow(a: Status): String =
      s"code = ${a.getCode}, description = ${a.getDescription}, cause = ${a.getCause.getMessage}"
  }
}
