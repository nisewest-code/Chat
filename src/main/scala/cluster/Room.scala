package cluster

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import util.CborSerializable

object Room {
  val PublicKey: ServiceKey[Command] = ServiceKey("RoomPublicKey")
  sealed trait Command
  final case class PushMessage(message: TransferMessage) extends Command with CborSerializable
  final case class LoginRoom(ref: ActorRef[Manager.Command]) extends Command with CborSerializable
  final case class LogoutRoom(ref: ActorRef[Manager.Command]) extends Command with CborSerializable
  final case class CloseRoom() extends Command

  object TypeRoom extends Enumeration {
    type TypeRoom = Value
    val Private, Public = Value
  }
  import cluster.Room.TypeRoom.TypeRoom
  def apply(key: BigInt, typeRoom: TypeRoom): Behavior[Command] = {
    Behaviors.setup { ctx =>
      val topic = ctx.spawn(Topic[Manager.Command](s"$key")
        , s"key")
      Behaviors.receiveMessage {
        case LogoutRoom(ref) =>
          ref ! Manager.LogoutRoom(key, typeRoom)
          topic ! Topic.Unsubscribe(ref)
          Behaviors.same
        case LoginRoom(ref) =>
          topic ! Topic.Subscribe(ref)
          ref ! Manager.InviteRoom(key, ctx.self, typeRoom)
          Behaviors.same
        case PushMessage(message) =>
          topic ! Topic.Publish(Manager.PostMessage(key, message))
          Behaviors.same
        case CloseRoom() =>
          topic ! Topic.Publish(Manager.LogoutRoom(key, typeRoom))
          Behaviors.stopped
      }
    }
  }
}
