package controllers

import javax.inject._

import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json }
import play.api.mvc._

import models.McpProtocol._

@Singleton
class McpController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  def health: Action[AnyContent] = Action {
    Ok(Json.toJson(Health("ok")))
  }

  def schema: Action[AnyContent] = Action {
    Ok(
      Json.toJson(
        Schema(
          name = "mcp-movies",
          version = "0.1.0",
          description = "MCP server to expose movie resources and a bookmarking tool"
        )
      )
    )
  }

  def listResources: Action[AnyContent] = Action {
    val resources = Seq.empty[Resource]
    Ok(Json.toJson(resources))
  }

  def callResource: Action[JsValue] = Action(parse.json) { req =>
    req.body.validate[ResourceCallRequest] match {
      case JsSuccess(call, _) =>
        call.name match {
          case other =>
            NotFound(
              Json.toJson(
                CallResult(
                  success = false,
                  data = Json.obj(
                    "error" -> s"Unknown resource: $other"
                  )
                )
              )
            )
        }

      case JsError(errs) =>
        BadRequest(
          Json.toJson(
            CallResult(
              success = false,
              data = Json.obj(
                "error" -> "Invalid JSON body for ResourceCallRequest",
                "details" -> errs.toString
              )
            )
          )
        )
    }
  }

}
