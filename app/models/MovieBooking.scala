package models

import play.api.libs.json.Json

case class MovieSelected(imdbId: String, screenId: String)

object MovieSelected {
  implicit val formats = Json.format[MovieSelected]
}

case class MovieInfoResponse(imdbId: String,
                             screenId: String,
                             movieTitle: String,
                             availableSeats: Int,
                             reservedSeats: Int)

object MovieInfoResponse {
  implicit val formats = Json.format[MovieInfoResponse]
}

case class MovieRegistrationRequest(imdbId: String, availableSeats: Int, screenId: String)

object MovieRegistrationRequest {
  implicit val formats = Json.format[MovieRegistrationRequest]
}

case class Movie(imdbId: String, title: String)

object Movie {
  implicit val formats = Json.format[Movie]
}

case class MovieRecord(movie: Movie,
                       screenId: String,
                       availableSeats: Int,
                       reservedSeats: Option[Int] = None) {
  def movieInfo = new MovieInfoResponse(movie.imdbId, screenId, movie.title, availableSeats, reservedSeats.getOrElse(0))
}

object MovieRecord {
  implicit val formats = Json.format[MovieRecord]
}


