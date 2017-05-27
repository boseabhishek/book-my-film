package services

import connectors.IMDBConnector
import models.{Movie, MovieRecord, MovieRegistrationRequest, MovieSelected}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import repositories._

import scala.concurrent.Future


class MovieTicketRegistrationAndReservationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockMovieRepository = mock[MovieBookingRepository]
  val mockImdbConnector = mock[IMDBConnector]

  object TestMovieRegistrationAndReservationService extends MovieRegistrationAndReservationService {
    val movieRepository: MovieBookingRepository = mockMovieRepository
    val imdbConnector: IMDBConnector = mockImdbConnector
  }

  override def beforeEach(): Unit = {
    reset(mockMovieRepository)
    reset(mockImdbConnector)
  }

  val movieRegistrationRequest = MovieRegistrationRequest("tt0111161", 100, "screen_123456")
  val invalidMovieRegistrationRequest = MovieRegistrationRequest("someImdbId", 100, "screen_123456")

  val movieReservationRequest = MovieSelected("tt0111161", "screen_123456")
  val noMatchMovieReservationRequest = MovieSelected("123456", "abcd")

  val movieRecord = MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 100, None)
  val movieRecordNoAvailableSeats = MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 0, None)

  val updatedMovieRecord = MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 99, Some(1))

  "MovieRegistrationAndReservationService" should {

    "save a movie" when {
      "record could NOT be fetched for the imdbId and screenId passed" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetchError))
        when(mockImdbConnector.getMovieTitle(Matchers.any())) thenReturn (Future.successful(Some("movie-title")))
        when(mockMovieRepository.create(Matchers.any())) thenReturn (Future.successful(MovieRegistrationCreated(movieRecord)))
        val record = MovieRegistrationCreated(MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 100, None))
        val result = TestMovieRegistrationAndReservationService.saveMovie(movieRegistrationRequest)
        await(result) must be(record)
      }
    }

    "fail to save a movie" when {
      "record could be fetched for the imdbId and screenId passed" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetched(movieRecord)))
        val result = TestMovieRegistrationAndReservationService.saveMovie(movieRegistrationRequest)
        await(result) must be(MovieRegistrationCreateError("For Movie 'The Shawshank Redemption', record already exists"))
      }
    }

    "throw RuntimeException" when {
      "valid title could not be found from 3rd Party IMDB" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetchError))
        when(mockImdbConnector.getMovieTitle(Matchers.any())) thenReturn (Future.successful(None))
        val thrown = the[RuntimeException] thrownBy await(TestMovieRegistrationAndReservationService.saveMovie(invalidMovieRegistrationRequest))
        thrown.getMessage must include("No movie title found for imdbid - someImdbId from IMDB")
      }
    }

    "modify and reserve a seat in the screen" when {
      "movie record could be found for reservation request and available seats more than 1" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetched(movieRecord)))
        when(mockMovieRepository.update(Matchers.any(), Matchers.any())) thenReturn (Future.successful(MovieBookingUpdated(updatedMovieRecord)))
        val result = TestMovieRegistrationAndReservationService.modifyBooking(movieReservationRequest)
        await(result) must be("One seat reserved at Screen - screen_123456")
      }
    }


    "fail to modify and reserve a seat in the screen" when {
      "movie record could be found for reservation request but no seats available" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetched(movieRecordNoAvailableSeats)))
        val result = TestMovieRegistrationAndReservationService.modifyBooking(movieReservationRequest)
        await(result) must include("Sorry! No more seats available for The Shawshank Redemption")
      }

      "movie record could be found for reservation request but update failed" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetched(movieRecord)))
        when(mockMovieRepository.update(Matchers.any(), Matchers.any())) thenReturn (Future.successful(MovieBookingUpdateFailed))
        val result = TestMovieRegistrationAndReservationService.modifyBooking(movieReservationRequest)
        await(result) must include("Movie seat reservation failed at screen screen_123456")
      }

      "movie record is NOT found in booking database" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetchError))
        val result = TestMovieRegistrationAndReservationService.modifyBooking(noMatchMovieReservationRequest)
        await(result) must be("No movie with IMDB ID 123456 found at abcd")
      }
    }

    "return movie booking info" when {
      "imdb id and screen id could be found in database" in {
        when(mockMovieRepository.findOne(Matchers.any())) thenReturn (Future.successful(MovieBookingInformationFetched(movieRecord)))
        val result = TestMovieRegistrationAndReservationService.getMovieInfo("tt0111161", "screen_123456")
        await(result) must be(MovieBookingInformationFetched(movieRecord))
      }
    }
  }

}
