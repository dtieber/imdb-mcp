package service

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.IteratorHasAsScala

import org.slf4j.LoggerFactory

import models.{ Failure, Movie }

class BookmarkService(movieService: MovieService) {
  private val logger = LoggerFactory.getLogger(classOf[BookmarkService])

  private val bookmarks = new ConcurrentLinkedQueue[Movie]()

  def bookmark(movieName: String): Either[Failure, Movie] = {
    logger.info(s"Attempting to bookmark movie: $movieName")
    movieService.getMovieByName(movieName) match {
      case Right(movie) =>
        bookmarks.add(movie)
        logger.info(s"Successfully bookmarked movie: ${movie.title}")
        Right(movie)
      case Left(failure) =>
        logger.warn(s"Failed to bookmark movie: $movieName. Reason: ${failure.message}")
        Left(failure)
    }
  }

  def listBookmarks(): List[Movie] = {
    val bookmarkList = bookmarks.iterator().asScala.toList
    logger.info(s"Listing bookmarks. Total count: ${bookmarkList.size}")
    bookmarkList
  }

  def resetBookmarks(): Unit = {
    logger.info("Resetting all bookmarks.")
    bookmarks.clear()
    logger.info("All bookmarks have been cleared.")
  }
}
