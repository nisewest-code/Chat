package cluster

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import cluster.Manager.PostMessage
import ui.ChatNode.Response
import util.{CborSerializable, Constants}


class Subscriber(response: Response) extends Actor with ActorLogging {

  import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

  private val mediator: ActorRef = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("public", self)

  def receive: PartialFunction[Any, Unit] = {
    case Subscriber.SendMessage(message) =>
      response.postMessage(message)
  }
}

object Subscriber{
  final case class SendMessage(message: Message) extends CborSerializable
}