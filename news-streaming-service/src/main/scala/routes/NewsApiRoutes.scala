package routes


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import common._

import domain.{HeadLines, Preference}
import jsonprotocol.NewsApiJsonProtocol
import service.NewsApiManager.FetchHeadLines


import scala.concurrent.{ExecutionContext}


/**
  * Created by AYON SANYAL on 28-05-2018.
  */
class NewsApiRoutes(newsApiManager: ActorRef)(implicit val executionContext: ExecutionContext) extends ApiRouteDefinition with NewsApiJsonProtocol {

  def routes(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer): Route = {
    import akka.http.scaladsl.server.Directives._

    pathPrefix("headlines") {
      pathEndOrSingleSlash {
        get {
          parameter('preference.*) { preference =>
            val sourcePreference = preference.toList.map(name => Preference(name))
            serviceAndComplete[Set[HeadLines]](FetchHeadLines(sourcePreference), newsApiManager)
          }
        }


      }

    }


  }
}