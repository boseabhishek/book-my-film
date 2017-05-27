package connectors

import mockws.MockWS
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class IMDBConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val validImdbId = "tt123456"
  val invalidImdbId = "tt111111"
  val successResponseJson = Json.parse( """{"Title":"The Shawshank Redemption","Year":"1994","Rated":"R","Released":"14 Oct 1994"}""")

  "IMDBConnector" should {

    "return the title of the movie" when {
      "valid imdbId is passed" in {
        val ws = MockWS {
          case (GET, "http://www.omdbapi.com/?i=tt123456&r=json") => Action {
            Ok(successResponseJson)
          }
        }
        new IMDBConnector(ws).getMovieTitle(validImdbId).map{ result =>
          result must be (Some("Movie Name"))
        }
      }
    }

    "throw Runtime Exception" when {
      "invalid imdbId is passed" in {
        val ws = MockWS {
          case (GET, "http://www.omdbapi.com/?i=tt111111&r=json") => Action {
            InternalServerError
          }
        }
        new IMDBConnector(ws).getMovieTitle(invalidImdbId).map{ result =>
          result must be (None)
        }
      }
    }

  }
}
