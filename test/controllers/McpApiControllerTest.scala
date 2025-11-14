package controllers

import scala.concurrent.duration._

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

class McpApiControllerTest extends PlaySpec with GuiceOneAppPerSuite with Injecting {

  private val controller = inject[McpApiController]
  private val timeout = 10.seconds

  private def post(json: JsValue): FakeRequest[JsValue] =
    FakeRequest(POST, "/mcp")
      .withHeaders("Content-Type" -> "application/json")
      .withBody(json)

  private def bodyOf(fr: scala.concurrent.Future[Result]): String =
    contentAsString(fr)(timeout)

  private def jsonOf(fr: scala.concurrent.Future[Result]): JsValue =
    Json.parse(bodyOf(fr))

  "McpApiController.mcp" should {

    "return initialize result with protocolVersion, serverInfo, capabilities" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "initialize",
        "id" -> 1,
        "params" -> Json.obj(
          "clientInfo" -> Json.obj("name" -> "test", "version" -> "0.0.1"),
          "capabilities" -> Json.obj(
            "tools" -> Json.obj(),
            "resources" -> Json.obj(),
            "prompts" -> Json.obj()
          )
        )
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      (jsonOf(resultF) \ "jsonrpc").as[String] mustBe "2.0"
      (jsonOf(resultF) \ "id").as[Int] mustBe 1
      val res = (jsonOf(resultF) \ "result").as[JsObject]
      (res \ "protocolVersion").as[String] must not be empty
      (res \ "serverInfo" \ "name").as[String] mustBe "mcp-movies"
      (res \ "serverInfo" \ "version").as[String] mustBe "0.1.0"
      (res \ "capabilities" \ "tools" \ "list").as[Boolean] mustBe true
      (res \ "capabilities" \ "tools" \ "call").as[Boolean] mustBe true
      (res \ "capabilities" \ "resources" \ "list").as[Boolean] mustBe true
      (res \ "capabilities" \ "resources" \ "read").as[Boolean] mustBe true
    }

    "list tools" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "tools/list",
        "id" -> 2
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val tools = (jsonOf(resultF) \ "result" \ "tools").as[JsArray]
      tools.value.length mustBe 1
      (tools(0) \ "name").as[String] mustBe "bookmark"
      (tools(0) \ "inputSchema" \ "properties" \ "movie" \ "type").as[String] mustBe "string"
    }

    "call bookmark tool successfully" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "tools/call",
        "id" -> 3,
        "params" -> Json.obj(
          "name" -> "bookmark",
          "arguments" -> Json.obj("movie" -> "Inception")
        )
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val res = (jsonOf(resultF) \ "result").as[JsObject]
      (res \ "message").as[String] must include("Bookmarked")
      (res \ "bookmarks").as[JsValue] must not be JsNull
    }

    "return error for bookmark tool missing parameter" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "tools/call",
        "id" -> 4,
        "params" -> Json.obj(
          "name" -> "bookmark",
          "arguments" -> Json.obj()
        )
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val err = (jsonOf(resultF) \ "error").as[JsObject]
      (err \ "code").as[Int] mustBe -32602
    }

    "list resources with uri" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "resources/list",
        "id" -> 5
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val res = (jsonOf(resultF) \ "result").as[JsObject]
      val resources = (res \ "resources").as[JsArray]
      resources.value.length must be >= 1
      (resources(0) \ "uri").as[String] mustBe "resource://movies/all"
      (resources(0) \ "mimeType").as[String] mustBe "application/json"
    }

    "read resource by uri and return contents with uri mimeType text" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "resources/read",
        "id" -> 6,
        "params" -> Json.obj("uri" -> "resource://movies/all")
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val contents = (jsonOf(resultF) \ "result" \ "contents").as[JsArray]
      contents.value.length mustBe 1
      (contents(0) \ "uri").as[String] mustBe "resource://movies/all"
      (contents(0) \ "mimeType").as[String] mustBe "application/json"
      (contents(0) \ "text").as[String] must include("movies")
    }

    "read resource by name and return contents" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "resources/read",
        "id" -> 7,
        "params" -> Json.obj("name" -> "getMovies")
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val contents = (jsonOf(resultF) \ "result" \ "contents").as[JsArray]
      contents.value.length mustBe 1
      (contents(0) \ "uri").as[String] mustBe "resource://movies/all"
    }

    "return method not found for unknown method" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "does/not/exist",
        "id" -> 8
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val err = (jsonOf(resultF) \ "error").as[JsObject]
      (err \ "code").as[Int] mustBe -32601
    }

    "return NoContent for notification initialized" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "2.0",
        "method" -> "initialized",
        "params" -> Json.obj()
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe NO_CONTENT
      contentType(resultF) mustBe empty
      bodyOf(resultF) mustBe ""
    }

    "return JSON-RPC error envelope when jsonrpc not 2.0 and id present" in {
      val reqJson = Json.obj(
        "jsonrpc" -> "1.0",
        "method" -> "initialize",
        "id" -> 9
      )

      val resultF = controller.mcp.apply(post(reqJson))

      status(resultF) mustBe OK
      val err = (jsonOf(resultF) \ "error").as[JsObject]
      (err \ "code").as[Int] mustBe -32600
    }
  }
}
