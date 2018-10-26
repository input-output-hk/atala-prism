package io.iohk.cef.network
import com.typesafe.config.Config
import io.iohk.cef.ContainerId

trait NetworkFacade {

  def configurations: Seq[(ContainerId, Config)]

  def getNetwork[T](containerId: ContainerId): Option[Network[T]]
}
