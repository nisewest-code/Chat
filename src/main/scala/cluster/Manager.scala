package cluster


import akka.actor.AddressFromURIString
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.typed.{Cluster, Join}
import cluster.Room.TypeRoom
import cluster.Room.TypeRoom.TypeRoom
import ui.ChatNode.Response
import ui.UserData
import util.{CborSerializable, Constants}

import scala.collection.mutable

object Manager {

  private var members: Set[ActorRef[Command]] = Set()
  private val privateRooms: mutable.Map[BigInt, ActorRef[Room.Command]] = mutable.Map()
  private val publicRooms: mutable.Map[BigInt, ActorRef[Room.Command]] = mutable.Map()
  private val storages: mutable.Map[String, ActorRef[StorageMessage.Command]] = mutable.Map()

  def apply(name: String, _seed: String, response: Response): Behavior[Command] = {
    Behaviors.setup { ctx =>
      val userData = UserData(name, s"${ctx.system.address.host.get}:${ctx.system.address.port.get}")
      val cluster = Cluster(ctx.system)
      var seed = _seed
      if (seed.isEmpty){
        seed = userData.ip
        cluster.manager ! Join(AddressFromURIString(s"akka://${Constants.chatName}@${userData.ip}"))
      } else {
        cluster.manager ! Join(AddressFromURIString(s"akka://${Constants.chatName}@$seed"))
      }

      response.postData(userData, seed)

      val Key: ServiceKey[Command] = ServiceKey[Command](s"akka://Chat@")
      ctx.system.receptionist ! Receptionist.Register(Key, ctx.self)
      val listingAdapter: ActorRef[Receptionist.Listing] = ctx.messageAdapter { listing =>
        if (listing.key == Key) {
          MemberChange(listing)
        } else if (listing.key == Room.PublicKey){
          PublicRoomChange(listing)
        } else {
          StorageMessageChange(listing)
        }

      }
      ctx.system.receptionist ! Receptionist.Subscribe(Key, listingAdapter)
      ctx.system.receptionist ! Receptionist.Subscribe(Room.PublicKey, listingAdapter)
      ctx.system.receptionist ! Receptionist.Subscribe(StorageMessage.Key, listingAdapter)

      val publicRoomKey = UserData("Public", userData.ip).hashCode()
      val publicRoom = ctx.spawn(Room(publicRoomKey, TypeRoom.Public), "Public")
      ctx.system.receptionist ! Receptionist.Register(Room.PublicKey, publicRoom)
      publicRoom ! Room.LoginRoom(ctx.self)


      storages += userData.ip -> ctx.spawnAnonymous(StorageMessage())

      Behaviors.receiveMessage {
        case StorageMessageChange(StorageMessage.Key.Listing(listing)) =>
          val diff = listing.toList.diff(storages.values.toList)
          diff.find(item => item.path.address.toString.contains(seed)) match {
            case Some(value) =>
              storages += seed -> value
            case None =>
          }
          Behaviors.same
        case PublicRoomChange(Room.PublicKey.Listing(listing)) =>
          val diff = listing.toList.diff(publicRooms.values.toList)
          diff.find(item => item.path.address.toString.contains(seed)) match {
            case Some(item) =>
              item ! Room.LoginRoom(ctx.self)
            case None =>
          }
          Behaviors.same
        case MemberChange(Key.Listing(listing)) =>
          val diff = listing.diff(members)
          for {
            xs <- diff if xs.path.address != ctx.self.path.address
          } {
            xs ! AddUser(userData)
          }
          members = listing
          Behaviors.same
        case AddUser(userData) =>
          response.addUser(userData)
          Behaviors.same
        case InviteRoom(key, ref, typeRoom) =>
          if (typeRoom == TypeRoom.Public){
            publicRooms += key -> ref
          } else {
            privateRooms += key -> ref
          }

          Behaviors.same
        case PostMessage(keyChat, message) =>
          if (message.userTo != userData){
            storages.get(seed) match {
              case Some(value) =>
                value ! StorageMessage.PutMessageHistory(keyChat, message.msg)
            }
            response.postMessage(keyChat, message)
          }
          Behaviors.same
        case RemoveUser(userData) =>
          response.removeUser(userData)
          Behaviors.same
        case LogoutRoom(key, typeRoom) => {
          if (typeRoom == TypeRoom.Public){
            publicRooms.remove(key)
          } else {
            privateRooms.remove(key)
          }
          Behaviors.same
        }
        case CloseCallback() =>
          for {
            xs <- members if xs.path.address != ctx.self.path.address
            xPrivate <- privateRooms
            xPublic <- publicRooms
          } {
            xs ! RemoveUser(userData)
            xPrivate._2 ! Room.LogoutRoom(ctx.self)
            xPrivate._2 ! Room.CloseRoom()
            xPublic._2 ! Room.LogoutRoom(ctx.self)
          }
          Behaviors.stopped
        case SendMessage(typeRoom, message) =>
          if (typeRoom == TypeRoom.Public){
            publicRooms.get(UserData("Public", seed).hashCode()) match {
              case Some(item) =>
                item ! Room.PushMessage(TransferMessage(message.userFrom, message.userTo, message.msg))
              case None =>
            }
          } else {
            val privateRoom = ctx.spawnAnonymous(Room(message.userTo.hashCode() + message.userFrom.hashCode(), TypeRoom.Private))
            privateRoom ! Room.LoginRoom(ctx.self)
            members.find(item => item.path.address.toString.contains(message.userTo.ip)) match {
              case Some(item) =>
                privateRoom ! Room.LoginRoom(item)
              case None =>
            }
            privateRoom ! Room.PushMessage(TransferMessage(message.userFrom, message.userTo, message.msg))
          }
          Behaviors.same
        case PostHistoryMessages(keyChat, messages) =>
          response.postHistoryMessages(keyChat, messages)
          Behaviors.same

        case RequestHistoryMessage(_userData: UserData) =>
          val key = userData.hashCode() + _userData.hashCode()
          storages.get(seed) match {
            case Some(value) =>
              value ! StorageMessage.GetMessageHistory(key, ctx.self)
            case None =>
          }
          Behaviors.same
      }

    }
  }

  sealed class Command extends CborSerializable
  private final case class MemberChange(listing: Receptionist.Listing) extends Command
  private final case class PublicRoomChange(listing: Receptionist.Listing) extends Command
  private final case class StorageMessageChange(listing: Receptionist.Listing) extends Command
  final case class CloseCallback() extends Command
  final case class RemoveUser(userData: UserData) extends Command
  final case class AddUser(userData: UserData) extends Command
  final case class PostMessage(keyChat: BigInt, message: TransferMessage) extends Command
  final case class SendMessage(typeRoom: TypeRoom, message: TransferMessage) extends Command
  final case class RequestHistoryMessage(userData: UserData) extends Command
  final case class PostHistoryMessages(keyChat: BigInt, messages: List[Message]) extends Command
  final case class InviteRoom(key: BigInt, roomRef: ActorRef[Room.Command], typeRoom: TypeRoom) extends Command
  final case class LogoutRoom(key: BigInt, typeRoom: TypeRoom) extends Command

}