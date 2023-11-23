package ui

import cluster.{Message, TransferMessage}
import javafx.application.Platform
import javafx.collections.{FXCollections, ObservableList}
import javafx.fxml.FXML
import javafx.scene.control.{Button, ListView, SelectionMode, TextArea}
import ui.ChatNode.Request
import ui.ViewPagerController.{Pager, TypeChat}
import ui.ViewPagerController.TypeChat.TypeChat


class ViewPagerController extends Pager{

  @FXML var btnSend: Button = _
  @FXML var textArea: TextArea = _

  @FXML private var listViewMessages: ListView[String] = _
  private val listMessages: ObservableList[String] = FXCollections.observableArrayList()

  private var request: Request = _
  private var userFrom: UserData = _
  private var userTo: UserData = _
  private var typeChat = TypeChat.Private

  def initialize(): Unit = {
    btnSend.setOnAction(_ => {
      onClickSend()
    })
    listViewMessages.setItems(listMessages)
    listViewMessages.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
  }

  @FXML
  private def onClickSend(): Unit = {
    val text: String = textArea.getText()
    if (text.nonEmpty){
      request.sendMessage(typeChat, cluster.TransferMessage(userTo, userFrom, cluster.Message(userFrom, text)))
      listMessages.add(s"${userFrom.name} : $text")
    }
    textArea.clear()
  }

  def initPager(_typeChat: TypeChat, _request: Request, _userTo: UserData, _userFrom: UserData): Unit = {
    typeChat = _typeChat
    request = _request
    userFrom = _userFrom
    userTo = _userTo
    request.requestHistory(userTo)
  }

  override def postMessage(message: TransferMessage): Unit = {
    Platform.runLater(() => {
      listMessages.add(s"${ message.msg.userFrom} : ${message.msg.msg}")
    })
  }
  import scala.jdk.CollectionConverters._
  override def postHistoryMessages(messages: List[Message]): Unit = {
    val listMessage = listMessages.asScala
    listMessages.clear()
    messages.foreach(message => listMessages.add(s"${message.userFrom.name} : ${message.msg}"))
    listMessage.foreach{ _ => listMessages.add(_)}
  }
}

object ViewPagerController{
  object TypeChat extends Enumeration {
    type TypeChat = Value
    val Private, Public = Value
  }
  trait Pager {
    def postMessage(message: TransferMessage): Unit
    def postHistoryMessages(messages: List[Message]): Unit
  }
}