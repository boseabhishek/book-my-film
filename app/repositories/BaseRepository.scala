package repositories

import play.modules.reactivemongo.ReactiveMongoComponents
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

abstract class BaseRepository[T] extends ReactiveMongoComponents {

  protected val collection: Future[JSONCollection]

}
