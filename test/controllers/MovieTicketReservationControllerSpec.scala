package controllers

import models.{Movie, MovieRecord}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers.{OK, status}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import repositories.{MovieBookingInformationFetchError, MovieBookingInformationFetched, MovieRegistrationCreateError, MovieRegistrationCreated}
import services.MovieRegistrationAndReservationService

import scala.concurrent.Future


class MovieTicketReservationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockMovieService = mock[MovieRegistrationAndReservationService]

  override def beforeEach(): Unit = {
    reset(mockMovieService)
  }

  val validRegisterMovieJson = Json.parse( """{"imdbId": "tt0111161", "availableSeats": 100, "screenId": "screen_123456"}""")

  val movieRecord = MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 100, None)

  val validReserveSeatJson = Json.parse( """{"imdbId": "tt0111161", "screenId": "screen_123456"}""")

  val seatBookedSuccessMessage = "One seat reserved"

  val invalidJson = Json.parse( """{"anything": "ghekihh"}""")

  val testMovieTicketReservationController = new MovieTicketReservationController(mockMovieService)

  "MovieTicketReservationController" should {

    "register a movie" when {
      "valid input Json is passed" in {
        when(mockMovieService.saveMovie(Matchers.any())) thenReturn(Future.successful(MovieRegistrationCreated(movieRecord)))
        val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = validRegisterMovieJson)
        val result = testMovieTicketReservationController.registerMovie.apply(fakeRequest)
        status(result) must be(OK)
      }
    }

    "fail to register a movie" when {
      "invalid input Json is passed" in {
        val testMovieTicketReservationController = new MovieTicketReservationController(mockMovieService)
        val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = invalidJson)
        val result = testMovieTicketReservationController.registerMovie.apply(fakeRequest)
        status(result) must be(BAD_REQUEST)
      }

      "insertion of record fails in Mongo" in {
        when(mockMovieService.saveMovie(Matchers.any())) thenReturn(Future.successful(MovieRegistrationCreateError("Error message")))
        val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = validRegisterMovieJson)
        val result = testMovieTicketReservationController.registerMovie.apply(fakeRequest)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "reserve a seat" when {
      "valid input Json is passed" in {
        when(mockMovieService.modifyBooking(Matchers.any())) thenReturn(Future.successful(seatBookedSuccessMessage))
        val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = validReserveSeatJson)
        val result = testMovieTicketReservationController.reserveSeat.apply(fakeRequest)
        status(result) must be(OK)
      }
    }

    "fail to reserve seat" when {
      "invalid input Json is passed" in {
        val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = invalidJson)
        val result = testMovieTicketReservationController.reserveSeat.apply(fakeRequest)
        status(result) must be(BAD_REQUEST)
      }
    }

    "show the movie booking info" when {
      "valid imdbid and screen id are passed" in {
        when(mockMovieService.getMovieInfo(Matchers.any(), Matchers.any())) thenReturn(Future.successful(MovieBookingInformationFetched(movieRecord)))
        val result = testMovieTicketReservationController.viewMovieBookingInfo("tt0111161", "screen_123456").apply(FakeRequest())
        status(result) must be(OK)
      }
    }

    "fail to show the movie booking info" when {
      "invalid imdbid and screen id are passed" in {
        when(mockMovieService.getMovieInfo(Matchers.any(), Matchers.any())) thenReturn(Future.successful(MovieBookingInformationFetchError))
        val result = testMovieTicketReservationController.viewMovieBookingInfo("imdbi1234", "screen1234").apply(FakeRequest())
        status(result) must be(NOT_FOUND)
      }
    }
  }

}
