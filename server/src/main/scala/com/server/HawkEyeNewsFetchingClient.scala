package com.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsDefined, Json}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/** The client which will consume from headline service.
  * Created by AYON SANYAL on 08-06-2018.
  */
object HawkEyeNewsFetchingClient {
  /**
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    lazy val config = ConfigFactory.load
    import system.dispatcher
    def fetchNewsForSources = {


      /**
        * Base uri for headlines Api Rest EndPoint
        */
      val url = config.getString("headLinesApiUrl")

      /**
        * It is considered as preferences set by user.This is a client for consuming a service i.e headlines
        * In real world scenario,this request should be prepared  by from clint side i.e UI.
        */
      val preferences = ListBuffer("cnn", "cnbc", "espn", "buzzfeed", "axios", "bbc-news", "daily-mail", "fortune", "fox-news", "google-news-ca")

      val urlNew = s"${url}preference=${preferences.head}"


      /**
        * A source of news Source preferences which refreshes it self after  stipulated time
        */
      val sourceOfPreferences = Source.tick(config.getInt("tickTimeStart").seconds, config.getInt("RefreshInterval").minutes, preferences -= preferences.head)

      /**
        * Mapping the preference to service url
        */
      val mapToURL = sourceOfPreferences.map(preferences => {
        preferences.fold(urlNew)((url, preference) => {
          s"${url}&preference=${preference}"
        })
      })


      /**
        * Mapping the url into Http Response by calling the headLines Web Service
        */
      val mapToRequest = mapToURL.map(urlForReq => {
        Http().singleRequest(Get(urlForReq))
      })

      /** Converting the response into newsFeeds i.e String representation of json */
      mapToRequest.runForeach {
        response =>
          response onComplete {

            case Success(HttpResponse(StatusCodes.OK, _, entity, _)) => {
              bodyToString(entity).map(jsValue => Json.parse(jsValue)) onComplete {
                case Success(newsFeed) => newsFeed \ "error" match {
                  case JsDefined(error) => println("Sorry ! No data found ")
                  case _ => (newsFeed \\ "response").map(Json.prettyPrint).foreach(println)
                }
              }
            }
          }
      }


    }


    /**
      * This method converts the response body into json String
      *
      * @param entity
      * @param executionContext
      * @return
      */
    def bodyToString(entity: ResponseEntity)(implicit executionContext: ExecutionContext): Future[String] = entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String)

    fetchNewsForSources

  }

}
