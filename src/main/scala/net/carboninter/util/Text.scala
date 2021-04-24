package net.carboninter.util

object Text {

  def hilight(string: String, terms: String*) = terms.fold(string) { case (acc, s) =>
    acc.replaceAll(s, Console.YELLOW_B + Console.BLACK + s + Console.RESET)
  }

}
