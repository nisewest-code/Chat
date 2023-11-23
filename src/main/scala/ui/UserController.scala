package ui

import cluster.{Message, TransferMessage}
import javafx.application.Platform
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{ListView, Tab, TabPane}
import javafx.scene.layout.VBox
import javafx.stage.Stage
import ui.UserController.Messaging
import ui.ViewPagerController.TypeChat
import ui.ViewPagerController.TypeChat.TypeChat
import util.Constants

import scala.jdk.CollectionConverters._

class UserController extends Messaging {
  @FXML private var tabPane: TabPane = _
  @FXML private var listView: ListView[UserData] = _

  private val listUsers: ObservableList[UserData] = FXCollections.observableArrayList()
  private val listTabs: ObservableList[TabCustom] = FXCollections.observableArrayList()

  private var chatNode: ChatNode = _
  private var userFrom: UserData = _
  private var seed: String = _
  private var stage: Stage = _

  @FXML
  def initialize(): Unit = {
    listView.setItems(listUsers)

    // подписка на закрытие табы
    val listenerTabPane = new ListChangeListener[Tab] {
      override def onChanged(c: ListChangeListener.Change[_ <: Tab]): Unit = {
        while (c.next) {
          for (tab: Tab <- c.getRemoved.asScala) {
            listTabs.removeIf(_.tab == tab)
          }
          //                      if (tab != null && !tabPane.getTabs.contains(tab)) {
          //                        listTabs.removeIf(_.tab eq tab)
          //                      }
        }
      }
    }
    tabPane.getTabs.addListener(listenerTabPane)

    listView.setOnMouseClicked(event => {
      val selectedIndex = listView.getSelectionModel.getSelectedIndex
      if (selectedIndex == -1) {
        event.consume()
      } else {
        val itemUser = listUsers.asScala(selectedIndex)
        tabPane.getSelectionModel.select(listTabs.asScala.find(item => item.userData.ip == itemUser.ip && item.userData.name == itemUser.name)
          .getOrElse(addTab(
            itemUser.hashCode() + userFrom.hashCode(),
            itemUser, userFrom)).tab)
      }
    })
  }

  def transferUserData(_stage: Stage, name: String, _seed: String): Unit = {
    stage = _stage
    //    userFrom = userData
    chatNode = ChatNode(name, _seed, this)
    stage.setOnCloseRequest(_ => {
      chatNode.stopChat()
      stage.close()
    })
    chatNode.startChat()
  }

  private def addTab(keyChat: BigInt, userTo: UserData, userFrom: UserData, isCloseable: Boolean = true, typeChat: TypeChat = TypeChat.Private): TabCustom = {
    val resource = UserUI.getClass.getResource(s"../${Constants.pagerLayout}")
    val loader = new FXMLLoader(resource)
    val root: VBox = loader.load()
    val tab: Tab = new Tab()
    val controller: ViewPagerController = loader.getController[ViewPagerController]
    controller.initPager(typeChat, chatNode, userTo, userFrom)
    tab.setText(userTo.name)
    tab.setClosable(isCloseable)
    tab.setContent(root)
    tabPane.getTabs.add(tab)
    val tabCustom = TabCustom(keyChat, userTo, controller, tab)
    listTabs.add(tabCustom)
    tabCustom
  }


  override def addUser(userData: UserData): Unit = {
    Platform.runLater(() => {
      listUsers.add(userData)
    })
  }

  override def removeUser(userData: UserData): Unit = {
    Platform.runLater(() => {
      listUsers.removeIf(item => item.ip == userData.ip && item.name == userData.name)
      listTabs.asScala.find(item => item.userData.ip == userData.ip && item.userData.name == userData.name) match {
        case Some(value) =>
          listTabs.remove(value)
          tabPane.getTabs.remove(value.tab)
        case None => {}
      }

    })
  }

  override def postMessage(keyChat: BigInt, message: TransferMessage): Unit = {
    Platform.runLater(() => {
      //      val itemB = if (message.userTo.name == "Public" && )
      listTabs.asScala.find(item => item.keyChat == keyChat)
        .getOrElse(addTab(keyChat, message.userTo, message.userFrom)).pager.postMessage(message)
    })
  }

  override def postHistoryMessages(keyChat: BigInt, messages: List[Message]): Unit = {
    Platform.runLater(() => {
      listTabs.asScala.find(item => item.keyChat == keyChat) match {
        case Some(value) =>
          value.pager.postHistoryMessages(messages)
      }
    })
  }

  override def postData(userData: UserData, _seed: String): Unit = {
    Platform.runLater(() => {
      seed = _seed
      stage.setTitle(s"Chat -  ${userData.name} : (${userData.ip})")
      userFrom = userData
      addTab(
        UserData("Public", seed).hashCode(), UserData("Public", seed), userData,
        isCloseable = false, typeChat = TypeChat.Public)
    })
  }
}

object UserController {
  trait Messaging {
    def postData(userData: UserData, seed: String): Unit

    def postMessage(keyChat: BigInt, message: TransferMessage): Unit

    def postHistoryMessages(keyChat: BigInt, messages: List[Message]): Unit

    def addUser(userData: UserData): Unit

    def removeUser(userData: UserData): Unit
  }
}