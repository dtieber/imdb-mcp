package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc._

import models.McpProtocol._
import service.{ BookmarkService, MovieService }

@Singleton
class McpController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  private val movieService = new MovieService
  private val bookmarkService = new BookmarkService(movieService)

  def health: Action[AnyContent] = Action {
    ResponseUtils.successResponse(Json.obj("status" -> "ok"))
  }

  def schema: Action[AnyContent] = Action {
    ResponseUtils.successResponse(
      Json.obj(
        "name" -> "mcp-movies",
        "version" -> "0.1.0",
        "description" -> "MCP server to expose movie resources and a bookmarking tool"
      )
    )
  }

  def listResources: Action[AnyContent] = Action {
    val getMovies = Resource(
      name = "getMovies",
      description = "Returns a list of movie titles",
      paramsSchema = Json.obj()
    )

    val resources = Seq(getMovies)
    ResponseUtils.successResponse(Json.obj("resources" -> Json.toJson(resources)))
  }

  def callResource: Action[JsValue] = Action(parse.json) { req =>
    validate[ResourceCallRequest](req.body) { call =>
      dispatchResource(call)
    }
  }

  private def validate[T](json: JsValue)(onValid: T => Result)(implicit reads: Reads[T]): Result =
    json.validate[T] match {
      case JsSuccess(value, _) => onValid(value)
      case JsError(errs) =>
        ResponseUtils.badRequestResponse(
          "Invalid JSON body for ResourceCallRequest",
          Some(errs.toString)
        )
    }

  private def dispatchResource(call: ResourceCallRequest): Result =
    call.name match {
      case "getMovies" => handleGetMovies(call)
      case other       => ResponseUtils.notFoundResponse(s"Unknown resource: $other")
    }

  private def handleGetMovies(call: ResourceCallRequest): Result = {
    val movies = movieService.getMovies
    ResponseUtils.successResponse(Json.obj("movies" -> movies))
  }

  def listTools: Action[AnyContent] = Action {
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
    validateAs[ToolCallRequest](req.body, "ToolCallRequest") { call =>
      dispatchTool(call)
    }
  }

  private def dispatchTool(call: ToolCallRequest): Result =
    call.name match {
      case "bookmark" => handleBookmark(call)
      case other      => ResponseUtils.notFoundResponse(s"Unknown tool: $other")
    }

  private def handleBookmark(call: ToolCallRequest): Result = {
    (call.params \ "movie").asOpt[String] match {
      case Some(movieName) =>
        bookmarkService.bookmark(movieName) match {
          case Right(bookmarkedMovie) =>
            ResponseUtils.successResponse(
              Json.obj(
                "message" -> s"Bookmarked '${bookmarkedMovie.title}'",
                "bookmarks" -> bookmarkService.listBookmarks()
              )
            )
          case Left(err) =>
            ResponseUtils.badRequestResponse(err.message)
        }
      case None =>
        ResponseUtils.badRequestResponse("Missing parameter 'movie'")
    }
  }

  private def validateAs[T](json: JsValue, label: String)(
    onValid: T => Result
  )(implicit reads: Reads[T]): Result =
    json.validate[T] match {
      case JsSuccess(value, _) => onValid(value)
      case JsError(errs) =>
        ResponseUtils.badRequestResponse(s"Invalid JSON body for $label", Some(errs.toString))
    }

}
