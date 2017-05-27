package services

import javax.inject._

import com.google.inject.ImplementedBy
import connectors.IMDBConnector
import models._
import play.api.libs.json.{JsObject, Json}
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MovieRegistrationAndReservationServiceImpl @Inject()(movieBookingRepository: MovieBookingRepository, imdbApiConnector: IMDBConnector)
  extends MovieRegistrationAndReservationService {
  val movieRepository = movieBookingRepository
  val imdbConnector = imdbApiConnector
}

@ImplementedBy(classOf[MovieRegistrationAndReservationServiceImpl])
trait MovieRegistrationAndReservationService {

  val movieRepository: MovieBookingRepository
  val imdbConnector: IMDBConnector

  def saveMovie(movieReq: MovieRegistrationRequest): Future[MovieRegistrationCreate] = {
    fetchRecordByImdbAndScreenId(movieReq.imdbId, movieReq.screenId) flatMap {
      case MovieBookingInformationFetched(m) =>
        Future.successful(MovieRegistrationCreateError(s"For Movie '${m.movie.title}', record already exists"))
      case MovieBookingInformationFetchError =>
        imdbConnector.getMovieTitle(movieReq.imdbId).flatMap { movieTitle =>
          val bookMovie = MovieRecord(Movie(imdbId = movieReq.imdbId,
            title = movieTitle.getOrElse(throw new RuntimeException(s"No movie title found for imdbid - ${movieReq.imdbId} from IMDB"))),
            screenId = movieReq.screenId,
            availableSeats = movieReq.availableSeats)
          movieRepository.create(bookMovie)
        }
    }
  }

  def modifyBooking(reserveMovie: MovieSelected): Future[String] = {
    fetchRecordByImdbAndScreenId(reserveMovie.imdbId, reserveMovie.screenId) flatMap {
      case MovieBookingInformationFetched(m) if (m.availableSeats > 0) =>
        updateSeatReservationByImdbAndScreenId(m.copy(availableSeats = m.availableSeats - 1, reservedSeats = Some(m.reservedSeats.getOrElse(0) + 1))) flatMap {
          case MovieBookingUpdated(updatedMovieBooking) =>
            Future.successful(s"One seat reserved at Screen - ${updatedMovieBooking.screenId}")
          case MovieBookingUpdateFailed =>
            Future.successful(s"Movie seat reservation failed at screen ${reserveMovie.screenId}")
        }
      case MovieBookingInformationFetched(m) =>
        Future.successful(s"Sorry! No more seats available for ${m.movie.title} at Screen - ${m.screenId}")
      case MovieBookingInformationFetchError => Future.successful(s"No movie with IMDB ID ${reserveMovie.imdbId} found at ${reserveMovie.screenId}")
    }
  }

  def getMovieInfo(imdbId: String, screenId: String): Future[MovieBookingInformationFetch] =
    fetchRecordByImdbAndScreenId(imdbId, screenId)

  private def fetchRecordByImdbAndScreenId(imdbId: String, screenId: String) =
    movieRepository.findOne(createDBQuery(imdbId, screenId))

  private def updateSeatReservationByImdbAndScreenId(updateMovieRecord: MovieRecord): Future[MovieBookingUpdateStatus] =
    movieRepository.update(updateMovieRecord, createDBQuery(updateMovieRecord.movie.imdbId, updateMovieRecord.screenId))

  private def createDBQuery(firstParam: String, secondParam: String): JsObject = {
    Json.obj(
      "movie.imdbId" -> firstParam,
      "screenId" -> secondParam)
  }

}