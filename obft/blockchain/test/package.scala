package obft.blockchain
package test

import obft.fakes

object Mock {

  def Hash[T](id: String): fakes.Hash[T] =
    fakes.Hash(id).asInstanceOf[fakes.Hash[T]]

}
