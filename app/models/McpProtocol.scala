package models

import play.api.libs.json._

object McpProtocol {

  case class Health(status: String)
  implicit val healthFormat: OFormat[Health] = Json.format[Health]

}
