package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.frontend.PlayJson
import io.iohk.cef.frontend.models._

object Codecs
    extends PlayJson.Formats
    with CommonCodecs
    with CryptoCodecs
    with ChimericCodecs
    with IdentityCodecs
    with DataItemCodecs
    with NetworkCodecs
