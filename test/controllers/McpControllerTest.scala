package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._

class McpControllerTest extends AnyWordSpec with Matchers {

  private val controller = new McpController(stubControllerComponents())

  "McpController" should {

    "return ok for health" in {
      val request = FakeRequest()

      val futureResult = controller.health.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      (json \ "data" \ "status").as[String] shouldEqual "ok"
    }

    "return ok schema with expected fields" in {
      val request = FakeRequest()

      val futureResult = controller.schema.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      val data = (json \ "data").as[JsObject]
      (data \ "name").as[String] shouldEqual "mcp-movies"
      (data \ "version").as[String] shouldEqual "0.1.0"
      (data \ "description")
        .as[String] shouldEqual "MCP server to expose movie resources and a bookmarking tool"
    }

    "listResources returns list of resources" in {
      val request = FakeRequest()

      val futureResult = controller.listResources.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      val resources = (json \ "data" \ "resources").as[JsArray]
      resources.value.nonEmpty shouldEqual true
      val first = resources.value.head.as[JsObject]
      (first \ "name").as[String] shouldEqual "getMovies"
    }

    "callResource(getMovies) returns movies" in {
      val request = FakeRequest().withBody(Json.obj("name" -> "getMovies", "params" -> Json.obj()))

      val futureResult = controller.callResource.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      (json \ "data" \ "movies").toOption.isDefined shouldEqual true
    }

    "listTools returns list of tools" in {
      val request = FakeRequest()

      val futureResult = controller.listTools.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      val tools = (json \ "data" \ "tools").as[JsArray]
      tools.value.nonEmpty shouldEqual true
      val first = tools.value.head.as[JsObject]
      (first \ "name").as[String] shouldEqual "bookmark"
    }

    "callTool(bookmark) returns message and bookmarks" in {
      val request = FakeRequest().withBody(
        Json.obj("name" -> "bookmark", "params" -> Json.obj("movie" -> "The Godfather"))
      )

      val futureResult = controller.callTool.apply(request)

      status(futureResult) shouldEqual Results.Ok.header.status
      val json = contentAsJson(futureResult)
      (json \ "success").as[Boolean] shouldEqual true
      (json \ "data" \ "message").as[String] should include("Bookmarked 'The Godfather'")
      val bookmarks = (json \ "data" \ "bookmarks").as[JsArray]
      bookmarks.value.nonEmpty shouldEqual true
      val firstTitle =
        bookmarks.value.head.as[JsObject].value.get("title").map(_.as[String]).getOrElse("")
      firstTitle shouldEqual "The Godfather"
    }

  }

}
