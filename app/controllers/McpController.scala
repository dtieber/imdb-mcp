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

}
