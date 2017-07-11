package repositories

import javax.inject._

import models.MovieRecord
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait MovieRegistrationCreate
case class MovieRegistrationCreated(bookMovie: MovieRecord) extends MovieRegistrationCreate
case class MovieRegistrationCreateError(cause: String) extends MovieRegistrationCreate

sealed trait MovieBookingInformation
case class MovieBookingInformationFetched(bookMovie: MovieRecord) extends MovieBookingInformation
case object MovieBookingInformationFetchError extends MovieBookingInformation

sealed trait MovieBookingUpdateStatus
case class MovieBookingUpdated(bookMovie: MovieRecord) extends MovieBookingUpdateStatus
case object MovieBookingUpdateFailed extends MovieBookingUpdateStatus

@Singleton
class MovieBookingRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends BaseRepository[MovieRecord] {

  override protected val collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection[JSONCollection]("movie-booking"))


  def create(m: MovieRecord): Future[MovieRegistrationCreate] = {
    collection flatMap { coll =>
      coll.insert[MovieRecord](m).map { writeResult =>
        writeResult.ok match {
          case true => MovieRegistrationCreated(m)
          case _ => MovieRegistrationCreateError("Movie could not be registered")
        }
      }.recover {
        case e => Logger.warn("Movie could not be registered", e)
          MovieRegistrationCreateError("Movie could not be registered")
      }
    }
  }

  def findOne(query: JsObject): Future[MovieBookingInformation] = {
    collection flatMap { coll =>
      coll.find(query).sort(Json.obj("_id" -> -1)).one[MovieRecord] map {
        case Some(m) => MovieBookingInformationFetched(m)
        case None => MovieBookingInformationFetchError
      }
    }
  }

  def update(m: MovieRecord, query: JsObject): Future[MovieBookingUpdateStatus] = {
    collection flatMap { coll =>
      coll.update(query, m, upsert = false).map { writeResult =>
        writeResult.ok match {
          case true => MovieBookingUpdated(m)
          case _ => MovieBookingUpdateFailed
        }
      }.recover {
        case e => Logger.warn("Booking could not be updated", e)
          MovieBookingUpdateFailed
      }
    }
  }
}
