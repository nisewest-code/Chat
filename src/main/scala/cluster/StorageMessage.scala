package cluster

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import util.CborSerializable

import scala.collection.mutable

object StorageMessage {
  val Key: ServiceKey[Command] = ServiceKey("StorageMessage")
  private val messages: mutable.Map[BigInt, mutable.Buffer[Message]] = mutable.Map()

  def apply(): Behavior[Command] = {
    Behaviors.setup { ctx =>
      ctx.system.receptionist ! Receptionist.Register(Key, ctx.self)
      Behaviors.receiveMessage {
        case PutMessageHistory(keyChat, msg) =>
          messages.get(keyChat) match {
            case Some(value) =>
              value += msg
            case None =>
              messages += keyChat -> mutable.Buffer(msg)
          }

          Behaviors.same
        case GetMessageHistory(keyChat, ref) =>
          messages.get(keyChat) match {
            case Some(value) =>
              ref ! Manager.PostHistoryMessages(keyChat, value.toList)
            case None =>
          }
          Behaviors.same
      }
    }
  }

  sealed trait Command

  final case class PutMessageHistory(keyChat: BigInt, msg: Message) extends Command with CborSerializable

  final case class GetMessageHistory(keyChat: BigInt, ref: ActorRef[Manager.PostHistoryMessages]) extends Command with CborSerializable
}
