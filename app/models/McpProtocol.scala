package models

import play.api.libs.json._

object McpProtocol {

  case class Health(status: String)
  implicit val healthFormat: OFormat[Health] = Json.format[Health]

  case class Schema(
    name: String,
    version: String,
    description: String
  )
  implicit val schemaFormat: OFormat[Schema] = Json.format[Schema]

}
