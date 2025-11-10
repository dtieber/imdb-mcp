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

  case class Resource(
    name: String,
    description: String,
    paramsSchema: JsObject
  )
  implicit val resourceFormat: OFormat[Resource] = Json.format[Resource]

  case class ResourceCallRequest(
    name: String,
    params: JsObject
  )
  implicit val resourceCallRequestFormat: OFormat[ResourceCallRequest] =
    Json.format[ResourceCallRequest]

  case class CallResult(
    success: Boolean,
    data: JsValue
  )
  implicit val callResultFormat: OFormat[CallResult] = Json.format[CallResult]

  case class Tool(
    name: String,
    description: String,
    paramsSchema: JsObject
  )
  implicit val toolFormat: OFormat[Tool] = Json.format[Tool]

  case class ToolCallRequest(
    name: String,
    params: JsObject
  )
  implicit val toolCallRequestFormat: OFormat[ToolCallRequest] = Json.format[ToolCallRequest]

}
