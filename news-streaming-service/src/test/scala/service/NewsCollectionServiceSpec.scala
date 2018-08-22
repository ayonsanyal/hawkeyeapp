package service

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import common.{CompleteResult, ErrorMessage, Failure, FailureType}
import domain.{HeadLines, NotFoundHeadLines, Preference}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import service.NewsApiManager.FetchHeadLines

import scala.collection.immutable.TreeSet

/**
  * Created by AYON SANYAL on 28-05-2018.
  */
class NewsCollectionServiceSpec extends TestKit(ActorSystem("NewsCollectionService")) with WordSpecLike
  with Matchers with BeforeAndAfterAll
  with ImplicitSender {

  import scala.concurrent.duration._

  val ResolveTimeout = 50 seconds
  val mockParent = TestProbe()

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }


  "NewsCollectionService" should {

    "Reply with CompleteResult for set of HeadLines  for valid set of preferred sources " +
      "when it receives message of type FetchHeadLines" in {
      val newsCollectionServiceMockActor = mockParent.childActorOf(Props(classOf[NewsCollectionService], mockParent.ref))
      mockParent.send(newsCollectionServiceMockActor, FetchHeadLines(List(Preference("cnn"), Preference("cnbc"))))
      val res = mockParent.expectMsgType[CompleteResult[Set[HeadLines]]](ResolveTimeout)
      res.value shouldBe a[Set[HeadLines]]
    }

    "Reply with CompleteResult  of HeadLines ( For valid set of preferred sources and set of NotFoundHeadLines " +
      "i.e for invalid sources when it receives message of type FetchHeadLines)" in {
      val newsCollectionServiceMockActor = mockParent.childActorOf(Props(classOf[NewsCollectionService], mockParent.ref))
      implicit val orderingForFailureSources = Ordering[String].on[NotFoundHeadLines](_.sourceName)

      mockParent.send(newsCollectionServiceMockActor, FetchHeadLines(List(Preference("cnn"), Preference("ayon"), Preference("sanyal"))))
      mockParent.expectMsgType[CompleteResult[Set[HeadLines]]](ResolveTimeout)
    }

    "Reply with NotFoundHeadLines " +
      "for the set of invalid sources when it receives message of type FetchHeadLines" in {
      val newsCollectionServiceMockActor = mockParent.childActorOf(Props(classOf[NewsCollectionService], mockParent.ref))
      implicit val orderingForFailureSources = Ordering[String].on[NotFoundHeadLines](_.sourceName)


      mockParent.send(newsCollectionServiceMockActor, FetchHeadLines(List(Preference("ayon"), Preference("sanyal"))))
      mockParent.expectMsg(ResolveTimeout, NewsCollectionService.failureResponse)
      mockParent.send(newsCollectionServiceMockActor, FetchHeadLines(List(Preference("1"), Preference("2"))))
      mockParent.expectMsg(ResolveTimeout, NewsCollectionService.failureResponse)

    }

    "Reply with Failure when it receives any message other than FetchHeadLines" in {
      val newsCollectionServiceMockActor = mockParent.childActorOf(Props(classOf[NewsCollectionService], mockParent.ref))
      mockParent.send(newsCollectionServiceMockActor, "Get News")
      mockParent.expectMsg(ResolveTimeout, NewsCollectionService.validationErrorResponse)
    }

  }

}
