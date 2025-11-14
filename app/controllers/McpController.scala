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
    req.body.validate[ResourceCallRequest] match {
      case JsSuccess(call, _) =>
        call.name match {
          case "getMovies" =>
            val movies = movieService.getMovies
            ResponseUtils.successResponse(Json.obj("movies" -> movies))

          case other =>
            ResponseUtils.notFoundResponse(s"Unknown resource: $other")
        }

      case JsError(errs) =>
        ResponseUtils.badRequestResponse(
          "Invalid JSON body for ResourceCallRequest",
          Some(errs.toString)
        )
    }
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
    req.body.validate[ToolCallRequest] match {
      case JsSuccess(call, _) =>
        call.name match {
          case "bookmark" =>
            val movieOpt = (call.params \ "movie").asOpt[String]
            movieOpt match {
              case Some(movie) =>
                bookmarkService.bookmark(movie) match {
                  case Right(bookmarkedMovie) =>
                    ResponseUtils.successResponse(
                      Json.obj(
                        "message" -> s"Bookmarked '${bookmarkedMovie.title}'",
                        "bookmarks" -> bookmarkService.listBookmarks()
                      )
                    )

                  case Left(error) =>
                    ResponseUtils.badRequestResponse(error.message)
                }

              case None =>
                ResponseUtils.badRequestResponse("Missing parameter 'movie'")
            }

          case other =>
            ResponseUtils.notFoundResponse(s"Unknown tool: $other")
        }

      case JsError(errs) =>
        ResponseUtils.badRequestResponse(
          "Invalid JSON body for ToolCallRequest",
          Some(errs.toString)
        )
    }
  }

}
