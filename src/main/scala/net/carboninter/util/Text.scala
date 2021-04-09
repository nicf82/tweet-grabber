package net.carboninter.util

object Text {

  def hilight(string: String, terms: String*) = terms.fold(string) { case (acc, s) =>
    acc.replaceAll(s, Console.GREEN_B + s + Console.RESET)
  }
}
