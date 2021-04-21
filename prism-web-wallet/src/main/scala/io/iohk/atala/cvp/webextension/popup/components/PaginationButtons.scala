package io.iohk.atala.cvp.webextension.popup.components

import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object PaginationButtons {

  case class Props(count: Int, pageNumber: Int, setPageNumber: Int => Unit)

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val previous = math.max(props.pageNumber - 1, 0)
    val next = math.min(props.pageNumber + 1, props.count - 1)

    div(className := "div__field_group_mui")(
      mui.IconButton.onClick(_ => props.setPageNumber(previous))(muiIcons.ChevronLeftOutlined()),
      s"${props.pageNumber + 1} of ${props.count}",
      mui.IconButton.onClick(_ => props.setPageNumber(next))(muiIcons.ChevronRightOutlined())
    )
  }
}
