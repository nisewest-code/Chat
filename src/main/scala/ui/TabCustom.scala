package ui

import javafx.scene.control.Tab

class TabCustom(val keyChat: BigInt, val userData: UserData, val pager: ViewPagerController.Pager, val tab: Tab)

object TabCustom{
  def apply(keyChat: BigInt, userData: UserData, pager: ViewPagerController.Pager, tab: Tab): TabCustom =
    new TabCustom(keyChat, userData, pager, tab)
}
