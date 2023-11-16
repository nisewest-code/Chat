package cluster

import ui.UserData
import util.CborSerializable

class Message(val userTo: UserData, val userFrom: UserData, val msg: String) extends CborSerializable

object Message{
  def apply(userTo: UserData, userFrom: UserData, msg: String): Message = new Message(userTo, userFrom, msg)
}
