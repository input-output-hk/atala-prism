package io.iohk.atala.cvp.webextension.common.models

final case class UserDetails(sessionId: String, name: String, role: String, logo: Array[Byte])
