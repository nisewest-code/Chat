package ui

class UserData(val name: String, val ip: String){
  override def toString: String = name
}

object UserData{
  def apply(name: String, ip: String): UserData = new UserData(name, ip)
}
