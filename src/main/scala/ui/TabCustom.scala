package ui

import javafx.scene.control.Tab

class TabCustom(val userData: UserData, val pager: ViewPagerController.Pager, val tab: Tab)

object TabCustom{
  def apply(userData: UserData, pager: ViewPagerController.Pager, tab: Tab): TabCustom = new TabCustom(userData, pager, tab)
}
