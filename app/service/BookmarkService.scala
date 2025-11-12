package service

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.IteratorHasAsScala

import models.{ Failure, Movie }

class BookmarkService(movieService: MovieService) {
  private val bookmarks = new ConcurrentLinkedQueue[Movie]()

  def bookmark(movieName: String): Either[Failure, Movie] = {
    movieService.getMovieByName(movieName) match {
      case Right(movie) =>
        bookmarks.add(movie)
        Right(movie)
      case Left(failure) =>
        Left(failure)
    }
  }

  def listBookmarks(): List[Movie] = bookmarks.iterator().asScala.toList

  def resetBookmarks(): Unit = bookmarks.clear()
}
