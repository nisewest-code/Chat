package ui

import cluster.{Manager, Message}
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import ui.ChatNode.{Request, Response}
import ui.UserController.Messaging
import util.Constants

class ChatNode(userData: UserData, seed: String, messaging: Messaging) extends ChatNode.Terminal with Request with Response{

  private var manager: ActorRef = _
  private var system: ActorSystem = _
  override def startChat(): Unit = {
    val hostname = userData.ip.substring(0, 9)
    val port = userData.ip.substring(10).toInt
    val customConf = ConfigFactory.parseString(
      s"""akka {
        actor {
          provider = "cluster"
          serialization-bindings {
                "util.CborSerializable" = jackson-cbor
          }
        }
        remote.artery {
          enabled = on
            transport = tcp
            canonical {
              hostname = \"$hostname\"
              port = $port
            }
          }

        cluster {
        akka.cluster.log-info = off
            seed-nodes = [
              "akka://${Constants.chatName}@$seed", "akka://${Constants.chatName}@${userData.ip}"]
            downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
            auto-down-unreachable-after = 5s
          }
      }""")
    system = ActorSystem(Constants.chatName, ConfigFactory.load(customConf))
    manager = system.actorOf(Props(classOf[Manager], userData, seed, this), s"${Constants.managerName}")
    manager ! Manager.Join()
  }

  override def stopChat(): Unit = {
    manager ! Manager.CloseCallback
    system.terminate()
  }

  override def sendMessage(message: Message): Unit = {
    manager ! Manager.SendMessage(message)
  }

  override def requestHistory(_userData: UserData): Unit = {
    manager ! Manager.RequestHistoryMessage(_userData)
  }

  override def postMessage(message: Message): Unit = {
    messaging.postMessage(message)
  }

  override def postHistoryMessages(_userData: UserData, messages: List[Message]): Unit = {
    messaging.postHistoryMessages(_userData, messages)
  }

  override def addUser(_userData: UserData): Unit = {
    messaging.addUser(_userData)

  }

  override def removeUser(_userData: UserData): Unit = {
    messaging.removeUser(_userData)
  }
}

object ChatNode {
  def apply(userData: UserData, seed: String, messaging: Messaging): ChatNode = new ChatNode(userData, seed, messaging)

  trait Terminal{
    def startChat(): Unit
    def stopChat(): Unit
  }
  trait Request{
    def sendMessage(message: Message): Unit
    def requestHistory(userData: UserData): Unit
  }
  trait Response{
    def addUser(userData: UserData): Unit
    def removeUser(userData: UserData): Unit
    def postMessage(message: Message): Unit
    def postHistoryMessages(userData: UserData, messages: List[Message]): Unit
  }
}
