package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import play.api.mvc.{ Result, Results }

class ResponseUtilsTest extends AnyWordSpec with Matchers {

  "ResponseUtils" should {

    "generate a success response" in {
      val data = Json.obj("key" -> "value")
      val result = ResponseUtils.successResponse(data)
      result.header.status shouldEqual Results.Ok.header.status
      result.body.contentType shouldEqual Some("application/json")
      val json = parseResultBody(result)
      (json \ "success").as[Boolean] shouldEqual true
      (json \ "data").as[JsObject] shouldEqual data
    }

    "generate a bad request response with only an error message" in {
      val errorMessage = "Invalid request"
      val result = ResponseUtils.badRequestResponse(errorMessage)
      result.header.status shouldEqual Results.BadRequest.header.status
      result.body.contentType shouldEqual Some("application/json")
      val json = parseResultBody(result)
      (json \ "success").as[Boolean] shouldEqual false
      (json \ "data" \ "error").as[String] shouldEqual errorMessage
      (json \ "data" \ "details").asOpt[String] shouldEqual None
    }

    "generate a bad request response with an error message and details" in {
      val errorMessage = "Invalid request"
      val details = "Missing required field"
      val result = ResponseUtils.badRequestResponse(errorMessage, Some(details))
      result.header.status shouldEqual Results.BadRequest.header.status
      result.body.contentType shouldEqual Some("application/json")
      val json = parseResultBody(result)
      (json \ "success").as[Boolean] shouldEqual false
      (json \ "data" \ "error").as[String] shouldEqual errorMessage
      (json \ "data" \ "details").as[String] shouldEqual details
    }

    "generate a not found response" in {
      val errorMessage = "Resource not found"
      val result = ResponseUtils.notFoundResponse(errorMessage)
      result.header.status shouldEqual Results.NotFound.header.status
      result.body.contentType shouldEqual Some("application/json")
      val json = parseResultBody(result)
      (json \ "success").as[Boolean] shouldEqual false
      (json \ "data" \ "error").as[String] shouldEqual errorMessage
    }
  }

  private def parseResultBody(result: Result): JsValue = {
    result.body match {
      case play.api.http.HttpEntity.Strict(data, _) =>
        Json.parse(data.utf8String)
      case _ =>
        throw new IllegalStateException("Body is not strict; test cannot parse non-strict body")
    }
  }
}
