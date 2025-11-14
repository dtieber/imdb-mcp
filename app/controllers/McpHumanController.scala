package controllers

import javax.inject._

import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc._

import models.McpProtocol._
import service.{ BookmarkService, MovieService }

@Singleton
class McpHumanController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  private val logger = LoggerFactory.getLogger(classOf[McpHumanController])

  private val movieService = new MovieService
  private val bookmarkService = new BookmarkService(movieService)

  def health: Action[AnyContent] = Action {
    logger.info("Health check endpoint called")
    ResponseUtils.successResponse(Json.obj("status" -> "ok"))
  }

  def schema: Action[AnyContent] = Action {
    logger.info("Schema endpoint called")
    ResponseUtils.successResponse(
      Json.obj(
        "name" -> "mcp-movies",
        "version" -> "0.1.0",
        "description" -> "MCP server to expose movie resources and a bookmarking tool"
      )
    )
  }

  def listResources: Action[AnyContent] = Action {
    logger.info("Listing available resources")
    val getMovies = Resource(
      name = "getMovies",
      description = "Returns a list of movie titles",
      paramsSchema = Json.obj()
    )

    val resources = Seq(getMovies)
    ResponseUtils.successResponse(Json.obj("resources" -> Json.toJson(resources)))
  }

  def callResource: Action[JsValue] = Action(parse.json) { req =>
    logger.info("Resource call endpoint called")
    validate[ResourceCallRequest](req.body) { call =>
      logger.info(s"Dispatching resource call: ${call.name}")
      dispatchResource(call)
    }
  }

  private def validate[T](json: JsValue)(onValid: T => Result)(implicit reads: Reads[T]): Result =
    json.validate[T] match {
      case JsSuccess(value, _) =>
        logger.info(s"Validation successful for JSON: $json")
        onValid(value)
      case JsError(errs) =>
        logger.warn(s"Validation failed for JSON: $json, Errors: $errs")
        ResponseUtils.badRequestResponse(
          "Invalid JSON body for ResourceCallRequest",
          Some(errs.toString)
        )
    }

  private def dispatchResource(call: ResourceCallRequest): Result =
    call.name match {
      case "getMovies" =>
        logger.info("Handling 'getMovies' resource")
        handleGetMovies(call)
      case other =>
        logger.warn(s"Unknown resource: $other")
        ResponseUtils.notFoundResponse(s"Unknown resource: $other")
    }

  private def handleGetMovies(call: ResourceCallRequest): Result = {
    logger.info("Fetching all movies")
    val movies = movieService.getMovies
    ResponseUtils.successResponse(Json.obj("movies" -> movies))
  }

  def listTools: Action[AnyContent] = Action {
    logger.info("Listing available tools")
    val bookmark = Tool(
      name = "bookmark",
      description = "Adds a movie title to the bookmark list",
      paramsSchema = Json.obj(
        "type" -> "object",
        "properties" -> Json.obj(
          "movie" -> Json.obj("type" -> "string")
        ),
        "required" -> Json.arr("movie"),
        "additionalProperties" -> false
      )
    )

    val tools = Seq(bookmark)
    ResponseUtils.successResponse(Json.obj("tools" -> Json.toJson(tools)))
  }

  def callTool: Action[JsValue] = Action(parse.json) { req =>
    logger.info("Tool call endpoint called")
    validateAs[ToolCallRequest](req.body, "ToolCallRequest") { call =>
      logger.info(s"Dispatching tool call: ${call.name}")
      dispatchTool(call)
    }
  }

  private def dispatchTool(call: ToolCallRequest): Result =
    call.name match {
      case "bookmark" =>
        logger.info("Handling 'bookmark' tool")
        handleBookmark(call)
      case other =>
        logger.warn(s"Unknown tool: $other")
        ResponseUtils.notFoundResponse(s"Unknown tool: $other")
    }

  private def handleBookmark(call: ToolCallRequest): Result = {
    (call.params \ "movie").asOpt[String] match {
      case Some(movieName) =>
        logger.info(s"Bookmarking movie: $movieName")
        bookmarkService.bookmark(movieName) match {
          case Right(bookmarkedMovie) =>
            logger.info(s"Successfully bookmarked movie: ${bookmarkedMovie.title}")
            ResponseUtils.successResponse(
              Json.obj(
                "message" -> s"Bookmarked '${bookmarkedMovie.title}'",
                "bookmarks" -> bookmarkService.listBookmarks()
              )
            )
          case Left(err) =>
            logger.warn(s"Failed to bookmark movie: $movieName. Reason: ${err.message}")
            ResponseUtils.badRequestResponse(err.message)
        }
      case None =>
        logger.warn("Missing parameter 'movie' in tool call")
        ResponseUtils.badRequestResponse("Missing parameter 'movie'")
    }
  }

  private def validateAs[T](json: JsValue, label: String)(
    onValid: T => Result
  )(implicit reads: Reads[T]): Result =
    json.validate[T] match {
      case JsSuccess(value, _) =>
        logger.info(s"Validation successful for $label: $json")
        onValid(value)
      case JsError(errs) =>
        logger.warn(s"Validation failed for $label: $json, Errors: $errs")
        ResponseUtils.badRequestResponse(s"Invalid JSON body for $label", Some(errs.toString))
    }

}
