package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc._

import models.{ JsonRpc, JsonRpcError, JsonRpcRequest, JsonRpcResponse }
import service.{ BookmarkService, MovieService }
import org.slf4j.LoggerFactory

@Singleton
class McpApiController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  private val logger = LoggerFactory.getLogger(classOf[McpApiController])

  private val movieService = new MovieService
  private val bookmarkService = new BookmarkService(movieService)

  private val protocolVersion: String = "2024-11-05"
  private val serverCapabilities: JsObject = Json.obj(
    "tools" -> Json.obj("list" -> true, "call" -> true),
    "resources" -> Json.obj("list" -> true, "read" -> true),
    "prompts" -> Json.obj()
  )

  def mcp: Action[JsValue] = Action(parse.json) { req =>
    logger.info("Received MCP request")
    req.body.validate[JsonRpcRequest] match {
      case JsError(errs) =>
        logger.warn(s"JSON validation error: $errs")
        val coerced: Seq[(JsPath, Seq[JsonValidationError])] =
          errs.iterator.map { case (p, es) => (p, es.toSeq) }.toSeq
        parseError(coerced)
      case JsSuccess(rpc, _) if rpc.jsonrpc != "2.0" =>
        logger.warn(s"Invalid JSON-RPC version: ${rpc.jsonrpc}")
        handleInvalidJsonRpcVersion(rpc)
      case JsSuccess(rpc, _) =>
        logger.info(s"Routing JSON-RPC method: ${rpc.method}")
        route(rpc)
    }
  }

  private def route(rpc: JsonRpcRequest): Result = {
    val isNotification = rpc.id.isEmpty
    val params = rpc.params.getOrElse(Json.obj())

    if (isNotification) {
      logger.info(s"Handling notification: ${rpc.method}")
      rpc.method match {
        case "initialized" =>
          logger.info("Handled 'initialized' notification")
          Results.NoContent
        case _ =>
          logger.warn(s"Unhandled notification method: ${rpc.method}")
          Results.NoContent
      }
    } else {
      logger.info(s"Handling request method: ${rpc.method}")
      rpc.method match {
        case "initialize" =>
          logger.info("Handling 'initialize' method")
          handleInitialize(rpc)
        case "tools/list" =>
          logger.info("Handling 'tools/list' method")
          handleToolsList(rpc)
        case "tools/call" =>
          logger.info("Handling 'tools/call' method")
          handleToolsCall(rpc, params)
        case "resources/list" =>
          logger.info("Handling 'resources/list' method")
          handleResourcesList(rpc)
        case "resources/read" =>
          logger.info("Handling 'resources/read' method")
          handleResourcesRead(rpc, params)
        case unknown =>
          logger.warn(s"Unknown method: $unknown")
          JsonRpc.err(rpc.id.get, -32601, s"Method not found: $unknown")
      }
    }
  }

  private def respondResult(
    rpc: JsonRpcRequest,
    payload: JsValue
  ): Result = {
    logger.info(s"Responding with result for method: ${rpc.method}")
    JsonRpc.ok(rpc.id.get, payload)
  }

  private def respondError(
    rpc: JsonRpcRequest,
    code: Int,
    msg: String,
    data: Option[JsValue] = None
  ): Result = {
    logger.warn(s"Responding with error: code=$code, message=$msg")
    JsonRpc.err(rpc.id.get, code, msg, data)
  }

  private def parseError(errs: Seq[(JsPath, Seq[JsonValidationError])]): Result = {
    logger.warn(s"Parse error: $errs")
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
  }

  private def handleInvalidJsonRpcVersion(rpc: JsonRpcRequest): Result = {
    logger.warn(s"Invalid JSON-RPC version: ${rpc.jsonrpc}")
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
    logger.info("Initializing server capabilities")
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

  private def handleToolsList(rpc: JsonRpcRequest): Result = {
    logger.info("Listing available tools")
    respondResult(rpc, Json.obj("tools" -> Json.arr(bookmarkToolDescriptor)))
  }

  private def handleToolsCall(
    rpc: JsonRpcRequest,
    params: JsValue
  ): Result = {
    logger.info(s"Calling tool with params: $params")
    val nameOpt = (params \ "name").asOpt[String]
    val args = (params \ "arguments").asOpt[JsObject].getOrElse(Json.obj())
    nameOpt match {
      case Some("bookmark") =>
        logger.info("Calling 'bookmark' tool")
        callBookmarkTool(rpc, args)
      case Some(other) =>
        logger.warn(s"Unknown tool: $other")
        respondError(rpc, -32601, s"Unknown tool: $other")
      case None =>
        logger.warn("Missing 'name' in tools/call")
        respondError(rpc, -32600, "Missing 'name' in tools/call")
    }
  }

  private def callBookmarkTool(
    rpc: JsonRpcRequest,
    args: JsObject
  ): Result = {
    (args \ "movie").asOpt[String] match {
      case None =>
        logger.warn("Missing parameter 'movie' in bookmark tool call")
        respondError(rpc, -32602, "Missing parameter 'movie'")
      case Some(movieName) =>
        logger.info(s"Bookmarking movie: $movieName")
        bookmarkService.bookmark(movieName) match {
          case Left(err) =>
            logger.warn(s"Failed to bookmark movie: $movieName. Reason: ${err.message}")
            respondError(rpc, -32602, err.message)
          case Right(bookmarked) =>
            logger.info(s"Successfully bookmarked movie: ${bookmarked.title}")
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

  private def handleResourcesList(rpc: JsonRpcRequest): Result = {
    logger.info("Listing available resources")
    respondResult(rpc, Json.obj("resources" -> Json.arr(getMoviesDescriptor)))
  }

  private def handleResourcesRead(
    rpc: JsonRpcRequest,
    params: JsValue
  ): Result = {
    logger.info(s"Reading resource with params: $params")
    resolveResourceUri(params) match {
      case None =>
        logger.warn("Missing 'uri' or 'name' in resources/read")
        respondError(rpc, -32600, "Missing 'uri' or 'name' in resources/read")
      case Some(MoviesAllUri) =>
        logger.info("Fetching all movies resource")
        val payload = buildMoviesContents()
        respondResult(rpc, payload)
      case Some(other) =>
        logger.warn(s"Unknown resource: $other")
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
