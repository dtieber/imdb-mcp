package service

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import models.{ Failure, Movie }

class BookmarkServiceTest
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  val mockMovieService: MovieService = mock[MovieService]
  var bookmarkService: BookmarkService = _

  override def beforeEach(): Unit = {
    bookmarkService = new BookmarkService(mockMovieService)
  }

  "BookmarkService" should {

    "successfully bookmark a movie when the movie exists" in {
      val movieName = "The Godfather"
      val movie = Movie(1, movieName)
      when(mockMovieService.getMovieByName(movieName)).thenReturn(Right(movie))

      val result = bookmarkService.bookmark(movieName)

      result shouldBe Right(movie)
      bookmarkService.listBookmarks() should contain(movie)
    }

    "return a failure when trying to bookmark a non-existent movie" in {
      val movieName = "Nonexistent Movie"
      val failure = Failure(s"Movie with name '$movieName' not found")
      when(mockMovieService.getMovieByName(movieName)).thenReturn(Left(failure))

      val result = bookmarkService.bookmark(movieName)

      result shouldBe Left(failure)
      bookmarkService.listBookmarks() shouldBe empty
    }

    "return an empty list when no movies are bookmarked" in {
      val bookmarks = bookmarkService.listBookmarks()

      bookmarks shouldBe empty
    }

    "return a list of all bookmarked movies" in {
      val movie1 = Movie(1, "The Godfather")
      val movie2 = Movie(2, "Pulp Fiction")
      when(mockMovieService.getMovieByName(movie1.title)).thenReturn(Right(movie1))
      when(mockMovieService.getMovieByName(movie2.title)).thenReturn(Right(movie2))
      bookmarkService.bookmark(movie1.title)
      bookmarkService.bookmark(movie2.title)

      val bookmarks = bookmarkService.listBookmarks()

      bookmarks should contain theSameElementsAs List(movie1, movie2)
    }

    "reset bookmarks to an empty list" in {
      val movie1 = Movie(1, "The Godfather")
      when(mockMovieService.getMovieByName(movie1.title)).thenReturn(Right(movie1))
      bookmarkService.bookmark(movie1.title)

      bookmarkService.resetBookmarks()

      bookmarkService.listBookmarks() shouldBe empty
    }
  }
}
