package common

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

object RejectionHandling {

  def customRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete((MethodNotAllowed, s"Not supported method! Supported methods are: ${names mkString ", "}!"))
    }
      .handleNotFound {
        extractUnmatchedPath { p =>
          complete((NotFound, s"The path you requested [$p] does not exist."))
        }
      }
      .result()
}
