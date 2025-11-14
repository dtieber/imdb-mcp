package models

import play.api.libs.json._
import play.api.mvc.{ Result, Results }

case class JsonRpcRequest(
  jsonrpc: String,
  method: String,
  id: Option[JsValue],
  params: Option[JsValue]
)
object JsonRpcRequest {
  implicit val reads: Reads[JsonRpcRequest] = Json.reads[JsonRpcRequest]
}

case class JsonRpcError(code: Int, message: String, data: Option[JsValue] = None)
object JsonRpcError {
  implicit val writes: Writes[JsonRpcError] = Json.writes[JsonRpcError]
}

case class JsonRpcResponse(
  jsonrpc: String = "2.0",
  id: JsValue,
  result: Option[JsValue] = None,
  error: Option[JsonRpcError] = None
)
object JsonRpcResponse {
  implicit val writes: Writes[JsonRpcResponse] = Json.writes[JsonRpcResponse]
}

object JsonRpc {
  def ok(id: JsValue, payload: JsValue): Result =
    Results.Ok(Json.toJson(JsonRpcResponse(id = id, result = Some(payload))))

  def err(id: JsValue, code: Int, msg: String, data: Option[JsValue] = None): Result =
    Results.Ok(Json.toJson(JsonRpcResponse(id = id, error = Some(JsonRpcError(code, msg, data)))))
}
