package models

import play.api.libs.json.{ Json, OFormat }

case class Movie(id: Int, title: String)
object Movie {
  implicit val format: OFormat[Movie] = Json.format[Movie]
}
