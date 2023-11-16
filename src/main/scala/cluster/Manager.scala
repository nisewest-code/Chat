package cluster


import akka.actor.{Actor, ActorLogging, ActorRef, AddressFromURIString, Props, RootActorPath}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Send, Subscribe}
import cluster.Manager._
import ui.ChatNode.Response
import ui.UserData
import util.{CborSerializable, Constants}

class Manager(userData: UserData, seed: String, response: Response) extends Actor with ActorLogging {
  private val cluster = Cluster(context.system)
  private val mediator: ActorRef = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(s"${Constants.managerName}", self)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember], classOf[MemberRemoved], classOf[MemberUp], classOf[MemberJoined])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {
    case MemberUp(member) =>
      val ref = context.actorSelection(s"${RootActorPath(member.address)}/user/${Constants.managerName}")
      ref ! AddUser(userData)

    case CloseCallback =>
      mediator ! Publish(s"${Constants.managerName}", RemoveUser(userData))

    case Join() =>
      cluster.join(AddressFromURIString(s"akka://${Constants.chatName}@$seed"))
          context.actorOf(Props(classOf[Destination], response), userData.ip)
          context.actorOf(Props(classOf[Subscriber], response))

    case SendMessage(message: Message) =>
      if (message.userTo.ip == seed && message.userTo.name == "Public") {
        // Public
        mediator ! Publish("public", Subscriber.SendMessage(message))
      } else {
        // Private
        mediator ! Send(path = s"/user/${Constants.managerName}/${message.userTo.ip}",
          Destination.SendMessage(message),
          localAffinity = true)
      }

    case PostMessage(message: Message) =>
      response.postMessage(message)

    case PostHistoryMessages(_userData: UserData, messages: List[Message]) =>
      response.postHistoryMessages(_userData, messages)

    case RequestHistoryMessage(_userData) => {}

    case AddUser(_userData) =>
      if (_userData.ip != userData.ip)
        response.addUser(_userData)

    case RemoveUser(_userData) =>
      response.removeUser(_userData)
  }
}

object Manager {
  sealed class Command extends CborSerializable

  final case class Join() extends Command

  final case class CloseCallback() extends Command

  final case class RemoveUser(userData: UserData) extends Command

  final case class AddUser(userData: UserData) extends Command

  final case class PostMessage(message: Message) extends Command

  final case class SendMessage(message: Message) extends Command

  final case class RequestHistoryMessage(userData: UserData) extends Command

  final case class PostHistoryMessages(userData: UserData, messages: List[Message]) extends Command
}