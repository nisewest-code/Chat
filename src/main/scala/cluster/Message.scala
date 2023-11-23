package cluster

import ui.UserData
import util.CborSerializable

class Message(val userFrom: UserData, val msg: String) extends CborSerializable

object Message{
  def apply(userFrom: UserData, msg: String): Message = new Message(userFrom, msg)
}
