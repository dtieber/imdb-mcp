package service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import models.{ Failure, Movie }

class MovieServiceTest extends AnyWordSpec with Matchers {

  "MovieService" should {

    val movieService = new MovieService()

    "return all movies when getMovies is called" in {
      val movies = movieService.getMovies

      movies should have size 17
      movies should contain(Movie(1, "The Godfather"))
      movies should contain(Movie(17, "Saving Private Ryan"))
    }

    "return a movie when getMovieByName is called with an existing movie name (case-insensitive)" in {
      val result = movieService.getMovieByName("The Godfather")

      result shouldBe Right(Movie(1, "The Godfather"))
    }

    "return a movie when getMovieByName is called with a name in a different case" in {
      val result = movieService.getMovieByName("the godfather")

      result shouldBe Right(Movie(1, "The Godfather"))
    }

    "return a Failure when getMovieByName is called with a non-existent movie name" in {
      val result = movieService.getMovieByName("Nonexistent Movie")

      result shouldBe Left(Failure("Movie with name 'Nonexistent Movie' not found"))
    }
  }
}
