package net.carboninter.models

import play.api.libs.json.Json

case class LiveTrack(track: String,  liveNames: List[String])
object LiveTrack {
  implicit val format = Json.format[LiveTrack]
}
case class  TwitterTermsCommand(liveTracks: List[LiveTrack])
object TwitterTermsCommand {
  implicit val format = Json.format[TwitterTermsCommand]
}
