package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.buttonButtonMod.ButtonProps
import com.alexitc.materialui.facade.materialUiCore.mod.PropTypes.Color
import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View.{DisplayMnemonic, OrganizationDetails}
import io.iohk.atala.cvp.webextension.popup.models.{Data, View}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class VerifyMnemonicWalletView extends Component {

  case class Props(
      backgroundAPI: BackgroundAPI,
      data: Data,
      switchToView: (View) => Unit
  )

  case class State(
      word1: String,
      word2: String,
      index1: Int,
      index2: Int,
      wordList: Seq[String],
      message: Option[String],
      isEnabled: Boolean
  )

  override def initialState: State = {
    val list = scala.util.Random.shuffle((0 until 12).toList).take(2).sorted
    val wordList = props.data.mnemonic.seed.split(" ").toSeq
    State("", "", list(0), list(1), wordList, None, false)
  }

  override def render(): ReactElement = {
    val wordLabel1 = s"word #${state.index1 + 1}"
    val wordLabel2 = s"word #${state.index2 + 1}"

    def enableButton = {
      if (state.isEnabled) {
        className := "btn_register"
      } else {
        className := "btn_register disabled"
      }
    }

    div(id := "verifyMnemonic", className := "status_container")(
      mui.IconButton.onClick(_ => props.switchToView(DisplayMnemonic))(muiIcons.ArrowBackSharp()),
      h3(className := "h3_register", id := "h3_register", "Account Registration"),
      div(className := "div__field_group")(
        h4(className := "h4_register")("Verify your recovery phrase")
      ),
      div(className := "div__field_group")(
        p(
          className := "description",
          id := "description2",
          "Enter the correct words of your recovery phrase below."
        )
      ),
      div(className := "div__field_group")(
        label(className := "_label")(wordLabel1),
        div(className := "input__container")(
          input(
            id := "word1",
            className := "_input",
            `type` := "text",
            value := state.word1,
            onChange := (e => setWord1(e.target.value)),
            onKeyUp := (e => matchWords1(e.target.value))
          ),
          word1Status()
        ),
        div(className := "div__field_group")(
          label(className := "_label")(wordLabel2),
          div(className := "input__container")(
            input(
              id := "word2",
              className := "_input",
              `type` := "text",
              value := state.word2,
              onChange := (e => setWord2(e.target.value)),
              onKeyUp := (e => matchWords2(e.target.value)),
              onKeyPress := { e =>
                if (e.key == "Enter") {
                  next()
                }
              }
            ),
            word2Status()
          )
        ),
        alertMessage(),
        div(className := "div__field_group")(
          div(
            id := "verifyNext",
            enableButton,
            onClick := { () =>
              next()
            }
          )("Next")
        )
      )
    )
  }

  private def alertMessage() = {
    val emptyElement = div()()

    val failMessage = (message: String) =>
      div(className := "div__field_group_mui")(
        mui.Button.withProps(
          ButtonProps()
            .setColor(Color.default)
            .setVariant(materialUiCoreStrings.outlined)
            .setClassName("button_fail_mui")
        )(muiIcons.Cancel().className("buttonIcon_fail").fontSize(materialUiCoreStrings.small))(message)
      )

    state.message match {
      case Some(m) => failMessage(m)
      case _ => emptyElement
    }

  }

  private def word1Status() = {
    if (state.word1 == state.wordList(state.index1)) {
      muiIcons.CheckCircle().className("buttonIcon_checkbox").fontSize(materialUiCoreStrings.small)
    } else {
      muiIcons.Cancel().className("buttonIcon_fail").fontSize(materialUiCoreStrings.small)
    }
  }

  private def word2Status() = {
    if (state.word2 == state.wordList(state.index2)) {
      muiIcons.CheckCircle().className("buttonIcon_checkbox").fontSize(materialUiCoreStrings.small)
    } else {
      muiIcons.Cancel().className("buttonIcon_fail").fontSize(materialUiCoreStrings.small)
    }
  }

  private def setWord1(newValue: String): Unit = {
    setState(_.copy(word1 = newValue))
  }

  private def setWord2(newValue: String): Unit = {
    setState(_.copy(word2 = newValue))
  }

  private def matchWords1(newValue: String): Unit = {
    if (newValue != state.wordList(state.index1) || state.word2 != state.wordList(state.index2)) {
      setState(_.copy(message = Some("Please enter correct words for the recovery phrase")))
    } else setState(_.copy(message = None, isEnabled = true))
  }

  private def matchWords2(newValue: String): Unit = {
    if (state.word1 != state.wordList(state.index1) || newValue != state.wordList(state.index2)) {
      setState(_.copy(message = Some("Please enter correct words for the recovery phrase")))
    } else setState(_.copy(message = None, isEnabled = true))
  }

  private def next(): Unit = {
    props.switchToView(OrganizationDetails(props.data))
  }

}
