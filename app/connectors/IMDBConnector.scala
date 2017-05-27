package connectors

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IMDBConnector @Inject()(ws: WSClient) {

  private val baseUri = "http://www.omdbapi.com"

  def getMovieTitle(imdbId: String): Future[Option[String]] = {
    val url = s"$baseUri/?i=$imdbId&r=json"
    val request: WSRequest = ws.url(url)
    Logger.info(s"[IMDBConnector][getIMDBInformation] - GET Uri -$url")

    request.get().map { resp =>
      resp.status match {
        case 200 =>
          val respJson = resp.json
          (respJson \ "Title").asOpt[String]
        case status =>
          Logger.warn(s"[IMDBConnector][getIMDBInformation] - status::$status for imdb id::$imdbId")
          None
      }
    }
  }

}
