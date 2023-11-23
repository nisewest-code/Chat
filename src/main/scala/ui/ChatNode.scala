package ui

import akka.actor.typed.ActorSystem
import cluster.{Manager, Message, TransferMessage}
import cluster.Manager.Command
import cluster.Room.TypeRoom
import com.typesafe.config.ConfigFactory
import sun.security.ec.ECDSAOperations.Seed
import ui.ChatNode.{Request, Response}
import ui.UserController.Messaging
import ui.ViewPagerController.TypeChat
import ui.ViewPagerController.TypeChat.TypeChat
import util.Constants

class ChatNode(name: String, seed: String, messaging: Messaging) extends ChatNode.Terminal with Request with Response{

  private var system: ActorSystem[Command] = _
  override def startChat(): Unit = {
      system = ActorSystem(Manager(name, seed, this), Constants.chatName, ConfigFactory.load())
  }

  override def stopChat(): Unit = {
    system ! Manager.CloseCallback()
    system.terminate()
  }

  override def sendMessage(typeChat: TypeChat, message: TransferMessage): Unit = {
    val typeRoom = if (typeChat == TypeChat.Private){
      TypeRoom.Private
    } else {
      TypeRoom.Public
    }
    system ! Manager.SendMessage(typeRoom, message)
  }

  override def requestHistory(_userData: UserData): Unit = {
    system ! Manager.RequestHistoryMessage(_userData)
  }

  override def postMessage(keyChat: BigInt, message: TransferMessage): Unit = {
    messaging.postMessage(keyChat, message)
  }

  override def postData(userData: UserData, seed: String): Unit = {
    messaging.postData(userData, seed)
  }

  override def postHistoryMessages(keyChat: BigInt, messages: List[Message]): Unit = {
    messaging.postHistoryMessages(keyChat, messages)
  }

  override def addUser(_userData: UserData): Unit = {
    messaging.addUser(_userData)

  }

  override def removeUser(_userData: UserData): Unit = {
    messaging.removeUser(_userData)
  }
}

object ChatNode {
  def apply(name: String, seed: String, messaging: Messaging): ChatNode = new ChatNode(name, seed, messaging)

  trait Terminal{
    def startChat(): Unit
    def stopChat(): Unit
  }
  trait Request{
    def sendMessage(typeChat: TypeChat, message: TransferMessage): Unit
    def requestHistory(userData: UserData): Unit
  }
  trait Response{
    def postData(userData: UserData, seed: String): Unit
    def addUser(userData: UserData): Unit
    def removeUser(userData: UserData): Unit
    def postMessage(keyChat: BigInt, message: TransferMessage): Unit
    def postHistoryMessages(keyChat: BigInt, messages: List[Message]): Unit
  }
}
