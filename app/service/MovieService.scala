package service

import org.slf4j.LoggerFactory

import models.{ Failure, Movie }

class MovieService {

  private val movies: List[Movie] = List(
    Movie(1, "The Godfather"),
    Movie(2, "The Dark Knight"),
    Movie(3, "Pulp Fiction"),
    Movie(4, "Inception"),
    Movie(5, "Fight Club"),
    Movie(6, "Forrest Gump"),
    Movie(7, "The Matrix"),
    Movie(8, "Interstellar"),
    Movie(9, "Parasite"),
    Movie(10, "Spirited Away"),
    Movie(11, "The Lord of the Rings"),
    Movie(12, "City of God"),
    Movie(13, "Se7en"),
    Movie(14, "Whiplash"),
    Movie(15, "Gladiator"),
    Movie(16, "The Silence of the Lambs"),
    Movie(17, "Saving Private Ryan")
  )

  private val logger = LoggerFactory.getLogger(classOf[MovieService])

  def getMovies: Seq[Movie] = {
    logger.info("Fetching the complete list of movies.")
    movies
  }

  def getMovieByName(name: String): Either[Failure, Movie] = {
    logger.info(s"Searching for a movie with name: '$name'")
    movies.find(_.title.equalsIgnoreCase(name)) match {
      case Some(movie) =>
        logger.info(s"Movie found: ${movie.title} (ID: ${movie.id})")
        Right(movie)
      case None =>
        logger.warn(s"Movie with name '$name' not found.")
        Left(Failure(s"Movie with name '$name' not found"))
    }
  }

}
