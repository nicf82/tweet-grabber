package net.carboninter.util

import net.carboninter.models.TwitterTermsCommand
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

import scala.util.{Failure, Success, Try}

trait JsonDecoder { self: Logging =>

  def decodeAs[T](jsonString: String)(implicit reads: Reads[T]) = Try(Json.parse(jsonString)) match {
    case Failure(exception) =>
      logger.error("Error parsing json" + exception)
      None
    case Success(value) =>
      value.validate[T] match {
        case JsError(errors) =>
          logger.error("Error parsing class. Errors: " + errors)
          None
        case JsSuccess(value, _) =>
          Some(value)
      }
  }
}
