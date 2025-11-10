package service

class MovieService {

  private val movies: Vector[String] = Vector(
    "The Godfather",
    "The Dark Knight",
    "Pulp Fiction",
    "Inception",
    "Fight Club",
    "Forrest Gump",
    "The Matrix",
    "Interstellar",
    "Parasite",
    "Spirited Away",
    "The Lord of the Rings",
    "City of God",
    "Se7en",
    "Whiplash",
    "Gladiator",
    "The Silence of the Lambs",
    "Saving Private Ryan"
  )

  def getMovies: Seq[String] = {
    movies
  }

}
