package routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import common._
import domain.{HeadLines, NewsSource, NewsTitle, NotFoundHeadLines}
import jsonprotocol.NewsApiJsonProtocol
import org.scalatest.{Matchers, WordSpecLike}
import spray.json.JsonFormat

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.concurrent.duration._
import akka.http.scaladsl.model.StatusCodes._
import utils.TestDataUtils

import scala.collection.immutable.TreeSet
import utils.TestDataUtils._

/**
  * Created by AYON SANYAL on 06-06-2018.
  */
class NewsApiRoutesSpec extends WordSpecLike with ScalatestRouteTest with Matchers with NewsApiJsonProtocol {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class scoping extends TestKit(system) with ImplicitSender {
    val apiManagerProbe = TestProbe()
    val mocknewsApiRoutesSuccess = new NewsApiRoutes(apiManagerProbe.ref) {
      override def serviceAndComplete[T: ClassTag](msg: Any, ref: ActorRef)(implicit format: JsonFormat[T]): Route = {
        import akka.pattern.ask
        import akka.http.scaladsl.server.Directives._
        val fut = (apiManagerProbe.ref ? msg).mapTo[ServiceResult[Set[HeadLines]]]
        apiManagerProbe.expectMsg(0 seconds, msg)

        val result = CompleteResult(dummyResponse)
        apiManagerProbe.reply(result)
        assert(fut.isCompleted && fut.value == Some(util.Success(result)))
        val responseFuture = Future.successful(result)
        val resp = ApiResponse(ApiResponseMetaData(OK.intValue), Some(dummyResponse))
        complete(resp)
      }
    }

    val mocknewsApiRoutesFailure = new NewsApiRoutes(apiManagerProbe.ref) {
      override def serviceAndComplete[T: ClassTag](msg: Any, ref: ActorRef)(implicit format: JsonFormat[T]): Route = {
        import akka.pattern.ask
        import akka.http.scaladsl.server.Directives._

        val fut = (apiManagerProbe.ref ? msg).mapTo[ServiceResult[TreeSet[NotFoundHeadLines]]]
        apiManagerProbe.expectMsg(0 seconds, msg)

        apiManagerProbe.reply(failureResponse)
        assert(fut.isCompleted && fut.value == Some(util.Success(failureResponse)))
        val responseFuture = Future.successful(failureResponse)

        val resp = ApiResponse[String](ApiResponseMetaData(BadRequest.intValue, Some(failureResponse.message)))
        complete(resp)
      }
    }

  }


  "NewsApiRoutes" should {

    s"respond with HTTP:-${OK} when the news items are fetched for valid source" in new scoping {

      val result = Get("/headlines?preference=cnn&preference=cnbc") ~> mocknewsApiRoutesSuccess.routes ~> runRoute
      check {
        status shouldBe OK
        responseAs[ApiResponse[Set[HeadLines]]] shouldEqual
          ApiResponse(ApiResponseMetaData(OK.intValue), Some(TestDataUtils.dummyResponse))
      }(result)

    }

    s"respond with HTTP:-${BadRequest} when the source is invalid" in new scoping {
      val url = "/headlines?preference=ayon&preference=sanyal"
      val result = Get(url) ~> mocknewsApiRoutesFailure.routes ~> runRoute
      check {

        responseAs[ApiResponse[String]] shouldEqual
          ApiResponse(ApiResponseMetaData(BadRequest.intValue, Some(nonReliableSource)))
      }(result)
    }

    /**
      * Below tests for the cases where rejections will happen
      */

    s"respond with HTTP-$NotFound for a non existing path" in new scoping {
      val url = "/headlines"
      val result = Get(url) ~> mocknewsApiRoutesSuccess.routes ~> runRoute
      check {
        status shouldBe NotFound
        responseAs[String] shouldBe "The path you requested [/non/existing/] does not exist."
      }
    }

    s"respond with HTTP-$MethodNotAllowed for a non supported HTTP method,Since in our api ,we are only dealing with Get,so the test results will focus on Get only" in new scoping {
      val url = "/headlines?preference=cnn&preference=cnbc"
      val result = Head(url) ~> mocknewsApiRoutesSuccess.routes ~> runRoute
      check {
        status shouldBe MethodNotAllowed
        responseAs[String] shouldBe "Not supported method! Supported methods are: GET!"
      }

    }


  }

}
