package controllers

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import models.McpProtocol.CallResult

object ResponseUtils {

  def successResponse(data: JsObject): Result = {
    Ok(Json.toJson(CallResult(success = true, data = data)))
  }

  def badRequestResponse(message: String, details: Option[String] = None): Result = {
    val errorData = Json.obj("error" -> message) ++
      details.map(d => Json.obj("details" -> d)).getOrElse(Json.obj())
    BadRequest(Json.toJson(CallResult(success = false, data = errorData)))
  }

  def notFoundResponse(message: String): Result = {
    NotFound(Json.toJson(CallResult(success = false, data = Json.obj("error" -> message))))
  }

}
