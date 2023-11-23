package cluster

import ui.UserData
import util.CborSerializable

class TransferMessage(val userTo: UserData, val userFrom: UserData, val msg: Message) extends CborSerializable

object TransferMessage{
  def apply(userTo: UserData, userFrom: UserData, msg: Message): TransferMessage = new TransferMessage(userTo, userFrom, msg)
}
