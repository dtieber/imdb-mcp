package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc._

import models.{ JsonRpc, JsonRpcError, JsonRpcRequest, JsonRpcResponse }
import service.{ BookmarkService, MovieService }

@Singleton
class McpApiController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  private val movieService = new MovieService
  private val bookmarkService = new BookmarkService(movieService)

  private val protocolVersion: String = "2024-11-05"
  private val serverCapabilities: JsObject = Json.obj(
    "tools" -> Json.obj("list" -> true, "call" -> true),
    "resources" -> Json.obj("list" -> true, "read" -> true),
    "prompts" -> Json.obj()
  )

  def mcp: Action[JsValue] = Action(parse.json) { req =>
    req.body.validate[JsonRpcRequest] match {
      case JsError(errs) =>
        val coerced: Seq[(JsPath, Seq[JsonValidationError])] =
          errs.iterator.map { case (p, es) => (p, es.toSeq) }.toSeq
        parseError(coerced)
      case JsSuccess(rpc, _) if rpc.jsonrpc != "2.0" =>
        handleInvalidJsonRpcVersion(rpc)
      case JsSuccess(rpc, _) =>
        route(rpc)
    }
  }

  private def route(rpc: JsonRpcRequest): Result = {
    val isNotification = rpc.id.isEmpty
    val params = rpc.params.getOrElse(Json.obj())

    if (isNotification) {
      rpc.method match {
        case "initialized" => Results.NoContent
        case _             => Results.NoContent
      }
    } else {
      rpc.method match {
        case "initialize"     => handleInitialize(rpc)
        case "tools/list"     => handleToolsList(rpc)
        case "tools/call"     => handleToolsCall(rpc, params)
        case "resources/list" => handleResourcesList(rpc)
        case "resources/read" => handleResourcesRead(rpc, params)
        case unknown          => JsonRpc.err(rpc.id.get, -32601, s"Method not found: $unknown")
      }
    }
  }

  private def respondResult(
    rpc: JsonRpcRequest,
    payload: JsValue
  ): Result =
    JsonRpc.ok(rpc.id.get, payload)

  private def respondError(
    rpc: JsonRpcRequest,
    code: Int,
    msg: String,
    data: Option[JsValue] = None
  ): Result =
    JsonRpc.err(rpc.id.get, code, msg, data)

  private def parseError(errs: Seq[(JsPath, Seq[JsonValidationError])]): Result =
    BadRequest(
      Json.obj(
        "jsonrpc" -> "2.0",
        "error" -> Json.obj(
          "code" -> -32700,
          "message" -> "Parse error",
          "data" -> JsString(errs.toString)
        )
      )
    )

  private def handleInvalidJsonRpcVersion(rpc: JsonRpcRequest): Result = {
    val isNotification = rpc.id.isEmpty
    if (isNotification) Results.NoContent
    else
      Results.Ok(
        Json.toJson(
          JsonRpcResponse(
            id = rpc.id.get,
            error = Some(JsonRpcError(-32600, "Invalid request: jsonrpc must be '2.0'"))
          )
        )
      )
  }

  private def handleInitialize(rpc: JsonRpcRequest): Result = {
    val payload = Json.obj(
      "protocolVersion" -> protocolVersion,
      "serverInfo" -> Json.obj("name" -> "mcp-movies", "version" -> "0.1.0"),
      "capabilities" -> serverCapabilities
    )
    respondResult(rpc, payload)
  }

  private def bookmarkToolDescriptor: JsObject =
    Json.obj(
      "name" -> "bookmark",
      "description" -> "Adds a movie title to the bookmark list",
      "inputSchema" -> Json.obj(
        "type" -> "object",
        "properties" -> Json.obj("movie" -> Json.obj("type" -> "string")),
        "required" -> Json.arr("movie"),
        "additionalProperties" -> false
      )
    )

  private def handleToolsList(rpc: JsonRpcRequest): Result =
    respondResult(rpc, Json.obj("tools" -> Json.arr(bookmarkToolDescriptor)))

  private def handleToolsCall(
    rpc: JsonRpcRequest,
    params: JsValue
  ): Result = {
    val nameOpt = (params \ "name").asOpt[String]
    val args = (params \ "arguments").asOpt[JsObject].getOrElse(Json.obj())
    nameOpt match {
      case Some("bookmark") => callBookmarkTool(rpc, args)
      case Some(other)      => respondError(rpc, -32601, s"Unknown tool: $other")
      case None             => respondError(rpc, -32600, "Missing 'name' in tools/call")
    }
  }

  private def callBookmarkTool(
    rpc: JsonRpcRequest,
    args: JsObject
  ): Result = {
    (args \ "movie").asOpt[String] match {
      case None =>
        respondError(rpc, -32602, "Missing parameter 'movie'")
      case Some(movieName) =>
        bookmarkService.bookmark(movieName) match {
          case Left(err) =>
            respondError(rpc, -32602, err.message)
          case Right(bookmarked) =>
            val payload = Json.obj(
              "message" -> s"Bookmarked '${bookmarked.title}'",
              "bookmarks" -> bookmarkService.listBookmarks()
            )
            respondResult(rpc, payload)
        }
    }
  }

  private val MoviesAllUri = "resource://movies/all"

  private def getMoviesDescriptor: JsObject =
    Json.obj(
      "name" -> "getMovies",
      "description" -> "Returns a list of movie titles",
      "uri" -> MoviesAllUri,
      "mimeType" -> "application/json",
      "paramsSchema" -> Json.obj()
    )

  private def handleResourcesList(rpc: JsonRpcRequest): Result =
    respondResult(rpc, Json.obj("resources" -> Json.arr(getMoviesDescriptor)))

  private def handleResourcesRead(
    rpc: JsonRpcRequest,
    params: JsValue
  ): Result = {
    resolveResourceUri(params) match {
      case None =>
        respondError(rpc, -32600, "Missing 'uri' or 'name' in resources/read")
      case Some(MoviesAllUri) =>
        val payload = buildMoviesContents()
        respondResult(rpc, payload)
      case Some(other) =>
        respondError(rpc, -32601, s"Unknown resource: $other")
    }
  }

  private def resolveResourceUri(params: JsValue): Option[String] = {
    val uriOpt = (params \ "uri").asOpt[String]
    val nameOpt = (params \ "name").asOpt[String]
    uriOpt.orElse(nameOpt.map {
      case "getMovies" => MoviesAllUri
      case other       => other
    })
  }

  private def buildMoviesContents(): JsObject = {
    val movies = movieService.getMovies
    val payloadJson = Json.obj("movies" -> movies)
    val payloadText = Json.stringify(payloadJson)
    Json.obj(
      "contents" -> Json.arr(
        Json.obj(
          "uri" -> MoviesAllUri,
          "mimeType" -> "application/json",
          "text" -> payloadText
        )
      )
    )
  }
}
