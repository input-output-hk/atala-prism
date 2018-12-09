package io.iohk.cef.codecs

import io.iohk.cef.codecs.nio.components._

package object nio extends NioCodecs {

  object auto extends NativeCodecs with ProductCodecs with OtherCodecs with CoproductCodecs
}
