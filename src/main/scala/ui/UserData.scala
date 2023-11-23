package ui

class UserData(val name: String, val ip: String){
  override def toString: String = name

  override def equals(obj: Any): Boolean = {
    obj match {
      case o: UserData => name == o.name && ip == o.ip
      case _ => false
    }
  }
  override def hashCode(): Int = {
    var total: Int = 31
    total = total * 31 + name.hashCode
    total = total * 31 + ip.hashCode
    total
  }
}

object UserData{
  def apply(name: String, ip: String): UserData = new UserData(name, ip)
}
