package controllers

import javax.inject._

import models.{MovieRegistrationRequest, MovieSelected}
import play.api.libs.json.Json
import play.api.mvc._
import repositories.{MovieBookingInformationFetchError, MovieBookingInformationFetched, MovieRegistrationCreateError, MovieRegistrationCreated}
import services.MovieRegistrationAndReservationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MovieTicketReservationControllerImpl @Inject()(movieRegistrationAndReservationService: MovieRegistrationAndReservationService)
  extends MovieTicketReservationController {
  val movieService = movieRegistrationAndReservationService
}

trait MovieTicketReservationController extends Controller {

  val movieService: MovieRegistrationAndReservationService

  def registerMovie = Action.async(parse.json) { implicit request =>
    request.body.asOpt[MovieRegistrationRequest] match {
      case Some(registerMovie) =>
        movieService.saveMovie(registerMovie) map {
          case MovieRegistrationCreated(m) =>
            Ok(s"Supplied movie of imdb id - ${m.movie.imdbId} and title - ${m.movie.title} have been registered.")
          case MovieRegistrationCreateError(e) =>
            InternalServerError(e)
        }
      case None => Future.successful(BadRequest)
    }
  }

  def reserveSeat = Action.async(parse.json) { implicit request =>
    request.body.asOpt[MovieSelected] match {
      case Some(reserveSeats) => movieService.modifyBooking(reserveSeats).map(resp => Ok(s"Reservation for requested movie (imdb id - ${reserveSeats.imdbId}) status:: $resp"))
      case None => Future.successful(BadRequest)
    }
  }

  def viewMovieBookingInfo(imdbId: String, screenId: String) = Action.async { implicit request =>
    movieService.getMovieInfo(imdbId, screenId).map {
      case MovieBookingInformationFetched(m) => Ok(Json.toJson(m.movieInfo))
      case MovieBookingInformationFetchError => NotFound(s"Sorry provided imdb id '$imdbId' or screen id '$screenId' not valid")
    }
  }

}