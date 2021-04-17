package net.carboninter.util

import net.carboninter.models.StubTweet

object Text {

  def hilight(string: String, terms: String*) = terms.fold(string) { case (acc, s) =>
    acc.replaceAll(s, Console.YELLOW_B + Console.BLACK + s + Console.RESET)
  }

  /**
   * Match if the tweet text contains track and name, unless the name term is abutted by a letter on both sides
   *
   * @param name Lowercase horse name
   * @param track Lowercase track name
   * @param lowerText Tweet text in lowercase
   * @return True if the tweet is considered a match
   */
  def matchEntryToTweet(name: String, track: String, lowerText: String) = {

    //These may be extended such as adding track synonyms - this wont work as twitter will only match the exaxt track anyway
    //def trackVariations(track: String) = Seq(track, track.filterNot(_==' '))

    def nameVariations(name: String) = Seq(name, name.filterNot(_==' '))

    if(lowerText.contains(track)) {

      val NameMatcher = (s"""(^|.*(.))(${nameVariations(name).mkString("|")})($$|(.).*)""").r

      lowerText match {
        case NameMatcher(before, _, name, after, _) =>
          !(before.lastOption.filter(_.isLetter).isDefined || after.headOption.filter(_.isLetter).isDefined)
        case _ => false
      }
    }
    else false
  }
}
