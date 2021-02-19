package io.iohk.atala.cvp.webextension.popup.components

import com.alexitc.materialui.facade.materialUiCore.anon._
import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import com.alexitc.materialui.facade.react.mod.ChangeEvent
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement, HTMLTextAreaElement}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.Hooks
import slinky.web.html._
import slinky.web.{SyntheticClipboardEvent, SyntheticFocusEvent, SyntheticKeyboardEvent}

import scala.scalajs.js.|

@react object ChipInput {
  case class Props(setChips: Seq[String] => Unit, validate: Option[Seq[String] => Unit] = None)
  val space = " "

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    val (values, setValues) = Hooks.useState(Seq.empty[String])
    val (onPaste, setOnPaste) = Hooks.useState(false)

    def onDelete(index: Int) = {
      setValues(values.filter(_ != values(index)))
      props.setChips(values)
    }

    def handleKeyDown(e: SyntheticKeyboardEvent[HTMLTextAreaElement | HTMLInputElement]) = {
      val inputElement = e.currentTarget.asInstanceOf[HTMLInputElement]
      val text = Option(inputElement.value)
      if (e.keyCode == KeyCode.Enter) {
        inputElement.value = ""
        if (text.exists(_.nonEmpty) && values.size < 12) {
          val chips = values :+ text.getOrElse("")
          setValues(chips)
          if (chips.size == 12) {
            e.preventDefault()
            props.setChips(chips)
          }
        }
      }
    }

    def handlePaste(e: SyntheticClipboardEvent[HTMLDivElement]) = {
      val data = e.clipboardData.getData("text").split(space).toList.take(12)
      setValues(data)
      props.setChips(data)
      setOnPaste(true)
    }

    def handleChange(e: ChangeEvent[HTMLTextAreaElement | HTMLInputElement]) = {
      if (onPaste)
        e.currentTarget.asInstanceOf[HTMLInputElement].value = ""
      setOnPaste(false)
    }

    def handleBlur(e: SyntheticFocusEvent[HTMLDivElement]) = {
      props.validate.map(_.apply(values))
    }

    div(className := "div__field_group")(
      div(className := "input__container")(
        mui
          .Card(
            mui
              .FormControl(
                values.zipWithIndex map {
                  case (v, i) =>
                    val word = s"${i + 1}. $v"
                    mui.Chip
                      .onDelete((_: Any) => onDelete(i))
                      .set("label", word)
                      .withKey(i.toString)
                      .classes(
                        PartialClassNameMapChipCl()
                          .setRoot("inputChip")
                          .setDeleteIcon("inputChipDelete")
                      )
                      .deleteIcon(mui.Icon()(img(src := "/assets/images/x.svg")))

                },
                mui
                  .Input()
                  .`type`("text")
                  .placeholder("Input here")
                  .classes(
                    PartialClassNameMapInputC()
                      .setInput("inputBtnChip")
                      .setRoot("inputBtnText")
                  )
                  .onKeyDown { e =>
                    handleKeyDown(e)
                  }
                  .onPaste { e =>
                    handlePaste(e)
                  }
                  .onChange { e =>
                    handleChange(e)
                  }
                  .onBlur { e => handleBlur(e) }
              )
              .classes(PartialClassNameMapFormCo().setRoot("formControl"))
          )
          .classes(PartialClassNameMapCardCl().setRoot("card"))
      )
    )

  }
}
