package repositories

import models.{Movie, MovieRecord}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.iteratee.Enumerator
import play.api.test.Helpers._
import reactivemongo.api.Cursor
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.{JSONCollection, JSONQueryBuilder}

import scala.collection.generic.CanBuildFrom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MovieTicketRepositorySpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val movieRecord = MovieRecord(Movie("tt0111161", "The Shawshank Redemption"), "screen_123456", 100, None)

  val mockCollection = mock[JSONCollection]

  val movieTicketRepository: MovieBookingRepository  = app.injector.instanceOf[MovieBookingRepository]

  "MovieTicketRepository" should {

    "create a movie record" when {

      "mongo insert is succesful" in {
        val result = await(movieTicketRepository.create(movieRecord))
        result must be (MovieRegistrationCreated(movieRecord))
      }

      "mongo insert fails" in {
        setupFindMockTemp
        when(mockCollection.indexesManager.create(Matchers.any())).thenReturn(Future.successful(UpdateWriteResult(true,0,0,Nil,Nil,None,None,None)))
        when(mockCollection.insert(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(UpdateWriteResult(false,0,0,Nil,Nil,None,None,None)))
        val result = await(movieTicketRepository.create(movieRecord))
        result must be (MovieRegistrationCreated(movieRecord))
      }


    }

  }

  private def setupFindMockTemp = {

    val queryBuilder = mock[JSONQueryBuilder]
    when(mockCollection.find(Matchers.any())(Matchers.any())) thenReturn queryBuilder
    val mockCursor = mock[Cursor[BSONDocument]]

    when(queryBuilder.cursor[BSONDocument](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenAnswer new Answer[Cursor[BSONDocument]] {
      def answer(i: InvocationOnMock) = mockCursor
    }

    when(queryBuilder.one(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

    when(
      mockCursor.collect[Traversable](Matchers.anyInt(), Matchers.anyBoolean())(Matchers.any[CanBuildFrom[Traversable[_], BSONDocument, Traversable[BSONDocument]]], Matchers.any[ExecutionContext])
    ) thenReturn Future.successful(List())

    when(
      mockCursor.enumerate()
    ) thenReturn Enumerator[BSONDocument]()
  }

}
