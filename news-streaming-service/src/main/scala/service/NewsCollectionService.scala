package service

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Outlet}
import akka.stream.scaladsl.{RunnableGraph, Source}
import com.typesafe.config.ConfigFactory
import service.NewsApiManager.FetchHeadLines
import akka.stream.scaladsl._
import akka.util.ByteString
import common._
import domain._
import play.api.libs.json.{JsDefined, Json}

import scala.collection.immutable.TreeSet
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}


object NewsCollectionService {
  lazy val config = ConfigFactory.load
  val nonReliableSource = ErrorMessage("wrong source name", Some(s"News not found for any source.  please try with valid source"))
  val failureResponse = Failure(FailureType.Validation, nonReliableSource)
  val wrongMessageType = ErrorMessage("wrong message type", Some(s"This type of message is not supported"))
  val validationErrorResponse = Failure(FailureType.Service, wrongMessageType)

  def props(parent: ActorRef) = Props(classOf[NewsCollectionService], parent)
}

/** Responsible for fetching news headlines from newsApi.org for set of Preferred Sources.After fetching ,
  * the successful and unsuccessful results are collected and sent back to sender.
  * This actor is independent and can be used as a child actor .
  * The reference of parent can be given in the primary constructor.
  * Created by AYON SANYAL on 28-05-2018.
  */
class NewsCollectionService(parent: ActorRef) extends Actor with ActorLogging {

  import NewsCollectionService._
  import context.dispatcher
  import scala.collection.immutable

  implicit val system = context.system
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))


  override def receive: Receive = customMessageHandlingToCollectNews


  /**
    * Case to deal with news fetching from 3rd party API
    *
    * @return
    */
  //
  def customMessageHandlingToCollectNews: Receive = {

    case FetchHeadLines(preference) => {
      import NewsCollectionService._

      /** Purpose of using TreeSet is to enhance the performance when the load or response elements will increase
        * in size. */
      implicit val orderingForHeadLine = Ordering[String].on[HeadLines](_.source.id)
      implicit val orderingForTitle = Ordering[String].on[NewsTitle](_.title)
      implicit val orderingForFoundSources = Ordering[String].on[NewsItemFound](_.sourceName)
      implicit val orderingForFailureSources = Ordering[String].on[NotFoundHeadLines](_.sourceName)


      /**
        * In case if sender information is needed for the scenario when the above message is forwarded to this actor.
        */
      val caller = sender

      /**
        * This method parse the jsonString and prepare instance of NewsItem out of it.
        *
        * @param jsonString
        * @return NewsItem
        */
      def parse(jsonString: String): NewsItem = {
        val jsValue = Json.parse(jsonString)
        jsValue \ "error" match {
          case JsDefined(error) => InvalidSource
          case _ => {
            val titles = (jsValue \\ "title")
            val titleSet = titles.map(title => NewsTitle(title.as[String])).toSet
            val sourceId = (jsValue \ "articles" \ (0) \ "source" \ "id").as[String]
            val sourceName = (jsValue \ "articles" \ (0) \ "source" \ "name").as[String]

            NewsItemFound(HeadLines(NewsSource(sourceId, sourceName), titleSet))
          }

        }
      }


      /**
        * Loading  the configuration for url and the api key for  "newsapi.org"
        */
      val newApiDomain = config.getString("newsApiUrl")
      val token = config.getString("newsApiKey")
      /**
        * The source will be a collection of news preferences for different sources.
        * Mapping the preferred source to LowerCase since the newsapi.org request uri is case sensitive.
        */
      //
      val newsSource: Source[Preference, NotUsed] = Source(preference.map(source => Preference(source.sources.toLowerCase)))
      /**
        * A client pool to support multiple requests
        */

      val poolClientFlow = Http().cachedHostConnectionPool[String]("newsapi.org")

      /**
        * Converts the HTTPResponse into the NewsItem.
        * The is either a Future of (Try[NewsItem])
        * Since the NewsApi.org only gives either success(200) orSuccess(400) so, the
        * exception scenario are not covered.
        * The parameter is a tuple of HTTPResponse and Request Param value.
        *
        * @param response
        * @return Future[(Try[NewsItem])]
        */
      def fetchNewsAsync(response: (Try[HttpResponse], String)): Future[(Try[NewsItem])] = {

        response match {
          case (Success(HttpResponse(StatusCodes.OK, _, entity, _)), value: String) => {
            val newsItemFuture = bodyToString(entity).map(parse)

            newsItemFuture map {

              case (newsItemFound: NewsItemFound) => {
                (util.Success(newsItemFound))
              }
            }

          }

          case _ => {

            Future(util.Success(NotFoundHeadLines(response._2)))
          }
        }
      }

      /** Sink which will evaluate the incoming items i.e a tuple  ,it folds the incoming items into future of tuple
        * which contains consolidated form i.e SET of NewsItemFound,NotFoundHeadLines,NewsTitle */
      val sinkForNewsStreaming = Sink.foldAsync[(immutable.TreeSet[NewsItemFound]
        , immutable.TreeSet[NotFoundHeadLines],
        immutable.TreeSet[NewsTitle]),
        Try[NewsItem]]((immutable.TreeSet.empty[NewsItemFound],
        immutable.TreeSet.empty[NotFoundHeadLines],
        immutable.TreeSet.empty[NewsTitle]))((newsItemFound, newsItem) => {

        newsItem match {
          case util.Success(items: NewsItemFound) => {

            /**
              * Only those news items are added for which the newsTitle is unique across all the sources.
              */
            val result = newsItemFound._1 + NewsItemFound(HeadLines(items.headLines.source, items.headLines.titles.filter(title => {
              !newsItemFound._3.contains(title)
            })))


            Future((result, newsItemFound._2, newsItemFound._3 ++ items.headLines.titles))
          }
          case util.Success(notFound: NotFoundHeadLines) => {
            Future(newsItemFound._1, newsItemFound._2 + notFound, newsItemFound._3)
          }
        }

      })


      /** It is a runnable graph of future of tuple of Set oF NewsItemsFound,
        * NotFoundHeadLines,NewsTitle.
        * The purpose of having three items in a tuple is to prepare a meaningful result from response which has following information :-
        *1. Tree Set of NewsItemFound wrapping headlines with unique titles across all the sources.
        *2. TreeSet of  NotFoundHeadLines wrapping the source id's for which news cannot be fetched.
        *3. NewsTitles wrapping unique news titles for all the sources for which the news titles are found.This treeSet is
        * ensuring that duplicate news from multiple sources will not be displayed.
        * *
        * The result from this graph will be used when it is materialised and it will run */

      def streamingNews: RunnableGraph[Future[(immutable.TreeSet[NewsItemFound]
        , immutable.TreeSet[NotFoundHeadLines],
        TreeSet[NewsTitle])]] = {

        /** Creating a request for newsApi.org for fetching the news from newsAPI.org corresponding to  sources in parallel. */
        val creatingRequest = newsSource.
          map(preference => {
            val sources = preference.sources
            val url = s"$newApiDomain$sources&apiKey=$token"
            (HttpRequest(uri = Uri(url)), preference.sources)
          })
          .via(poolClientFlow)

        /**
          * Mapping the HTTPResponse to Future of NewsItem and then materialising the flow.
          */

        creatingRequest.mapAsync(4)(fetchNewsAsync).toMat(sinkForNewsStreaming)(Keep.right)
      }


      /** Preparing the result which will be  sent back  to the requesting actor when the future completes. */
      streamingNews.run.onComplete {
        /** Case where news is found for all the sources */
        case util.Success(newsItems: (TreeSet[NewsItemFound], TreeSet[NotFoundHeadLines], TreeSet[NewsTitle])) if (!newsItems._1.isEmpty && newsItems._2.isEmpty) => {
          val headLines = newsItems._1.map(item => item.headLines)
          parent ! CompleteResult(headLines)
        }

        /** Case where news is found for some sources and not found for some sources */
        case util.Success(newsItems: (TreeSet[NewsItemFound], TreeSet[NotFoundHeadLines], TreeSet[NewsTitle])) if (!newsItems._1.isEmpty && !newsItems._2.isEmpty) => {
          var headLines: Set[HeadLines] = newsItems._1.map(item => item.headLines)
          val headLinesNotFound = newsItems._2.map(items => NotFoundForSource(items.sourceName))
          val newsTitleForFailedResource = Set(NewsTitle("News not found for  this source"))
          for {
            notFoundHeadLine <- headLinesNotFound
          } yield (headLines += HeadLines(NewsSource(notFoundHeadLine.sourceName, notFoundHeadLine.sourceName), newsTitleForFailedResource))

          parent ! CompleteResult(headLines)
        }

        /** Case where news is not found for any sources */
        case util.Success(newsItems: (TreeSet[NewsItemFound], TreeSet[NotFoundHeadLines], TreeSet[NewsTitle])) if (newsItems._1.isEmpty && !newsItems._2.isEmpty) => {


          parent ! failureResponse
        }


      }

    }

    case _ => parent ! validationErrorResponse

  }


  /**
    * This method converts the response body into json String
    *
    * @param entity
    * @param executionContext
    * @return
    */
  private def bodyToString(entity: ResponseEntity)(implicit executionContext: ExecutionContext): Future[String] = entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String)


}