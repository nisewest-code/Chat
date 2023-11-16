package ui

import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import util.Constants


class LoginController {
  @FXML var root: AnchorPane = _
  @FXML var fieldSeedIP, fieldIP, fieldName: TextField = _
  private var stage: Stage = _

  @FXML
  def onClickLogin(mouseEvent: MouseEvent): Unit = {
    val name: String = fieldName.getText()
    val seed: String = fieldSeedIP.getText()
    val ip: String = fieldIP.getText()
    if (name == "" || seed == "" || ip == "") {
      val alert: Alert = new Alert(AlertType.INFORMATION)
      alert.setTitle("Ошибка")
      alert.setHeaderText("Пустые поля")
      alert.setContentText("Заполните пустые поля")
      alert.showAndWait()
    } else {
      val userData = UserData(name, ip)
      val resource = getClass.getResource(s"../${Constants.mainLayout}")
      val loader = new FXMLLoader(resource)
      val root = loader.load[Parent]
      val controller: UserController = loader.getController[UserController]
      stage.setTitle(s"Chat -  $name : ($ip)")
      stage.setScene(new Scene(root, 600, 400))
      controller.transferUserData(stage, userData, seed)
    }
  }

  def transferData(_stage: Stage): Unit = {
    stage = _stage
  }
}