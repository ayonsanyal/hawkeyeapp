package service

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import domain.Preference
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import service.NewsApiManager.FetchHeadLines


/**
  * Created by AYON SANYAL on 28-05-2018.
  */
class NewsApiManagerSpec extends TestKit(ActorSystem("NewsApiManagerSpec")) with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  import scala.concurrent.duration._

  val ResolveTimeout = 50 seconds

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class scoping extends TestKit(system) with ImplicitSender {

    val newsApiManagerProbe = TestProbe()

    val newsCollectionServiceMock = TestProbe()
    val newsApiManagerTestActor = TestActorRef(new NewsApiManager {

      override def newsCollectionService(parent: ActorRef) = newsCollectionServiceMock.ref
    })

  }

  "NewsApiManager" should {

    "Forward every incoming message to NewsCollectionService actor when the message of type FetchHeadLines arrives " in new scoping {
      val request = FetchHeadLines(List(Preference("cnn"), Preference("cnbc")))
      newsApiManagerTestActor ! request
      newsCollectionServiceMock.expectMsg(ResolveTimeout, request)
    }

    "Reply with Failure when it receives any message other than FetchHeadLines" in new scoping {
      newsApiManagerTestActor ! "GetNews"
      expectMsg(ResolveTimeout, NewsCollectionService.validationErrorResponse)
    }


  }

}
