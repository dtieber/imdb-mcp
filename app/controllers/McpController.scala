package controllers

import javax.inject._

import play.api.libs.json.Json
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

}
