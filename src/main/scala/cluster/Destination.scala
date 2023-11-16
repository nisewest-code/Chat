package cluster


import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import ui.ChatNode.Response
import util.CborSerializable

class Destination(response: Response) extends Actor with ActorLogging {

  import akka.cluster.pubsub.DistributedPubSubMediator.Put

  private val mediator: ActorRef = DistributedPubSub(context.system).mediator

  mediator ! Put(self)


  def receive: PartialFunction[Any, Unit] = {
    case Destination.SendMessage(message) =>
//      mediator ! Publish(s"${Constants.managerName}", PostMessage(message))
      response.postMessage(Message(message.userFrom, message.userTo, message.msg))
  }
}

object Destination {
  final case class SendMessage(message: Message) extends CborSerializable
}