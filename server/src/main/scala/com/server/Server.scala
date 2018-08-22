package com.server

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import common.{ApiRouteDefinition, Bootstrap, RejectionHandling}
import routes.NewsApiRoutes
import service.NewsApiManager

/**
  * Created by AYON SANYAL on 28-05-2018.
  */
object Server extends App {

  override def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("NewsApiApp")
    implicit val mater = ActorMaterializer()
    val log = Logging(system.eventStream, "Server")
    import system.dispatcher
    import RejectionHandling._
    lazy val config = ConfigFactory.load
    val bootNewsApi = new Bootstrap {
      /**
        * Since only one module is involved,so overriding with anonymous class .Otherwise every module is supposed to have
        * its own bootstrap class with overridden bootup method.
        *
        * @param system The actor system to boot actors into
        * @return a List of Movie Services to be added into server
        */

      override def bootup(system: ActorSystem): List[ApiRouteDefinition] = {
        import system.dispatcher

        val newsApiManager = system.actorOf(Props[NewsApiManager])
        List(new NewsApiRoutes(newsApiManager))
      }
    }

    val routes = bootNewsApi.bootup(system).map(_.routes).reduce(_ ~ _)
    val prefixForVersion = pathPrefix(config.getString("apiVersion"))(routes)
    val prefixForApi = pathPrefix(config.getString("apiName"))(prefixForVersion)
    val routeWithRejectionHandler = handleRejections(customRejectionHandler)(prefixForApi)
    val serverPort = config.getInt("port")
    val serverSource = Http().bind(interface = config.getString("domain"), port = serverPort)

    val sink = Sink.foreach[Http.IncomingConnection](_.handleWith(routeWithRejectionHandler))
    serverSource.to(sink).run


  }

}
