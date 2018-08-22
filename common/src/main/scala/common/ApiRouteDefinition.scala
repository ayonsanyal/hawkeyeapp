package common

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import spray.json.JsonFormat
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import akka.http.scaladsl.model.StatusCodes._
import com.typesafe.config.ConfigFactory

/**
  * Companion of ApiRoutesDefinition
  */
object ApiRoutesDefinition {
  lazy val config = ConfigFactory.load
  val NotFoundResp = ApiResponse[String](ApiResponseMetaData(NotFound.intValue, Some(ErrorMessage("notfound"))))
  val UnexpectedFailResp = ApiResponse[String](ApiResponseMetaData(InternalServerError.intValue, Some(ServiceResult.UnexpectedFailure)))
}

/** A BluePrint to be followed by every underneath service
  * Created by AYON SANYAL on 28-05-2018.
  */
trait ApiRouteDefinition extends ApiResponseProtocol {

  import concurrent.duration._
  import ApiRoutesDefinition._

  implicit val endpointTimeout = Timeout(config.getInt("ApplicationTimeOut") seconds)


  /**
    * Returns the routes defined for this endpoint
    *
    * @param system The implicit system to use for building routes
    * @param ec     The implicit execution context to use for routes
    * @param mater  The implicit materializer to use for routes
    */
  def routes(implicit system: ActorSystem, ec: ExecutionContext, mater: Materializer): Route

  /**
    * Uses service to get a result and then inspects that result to complete the request
    *
    * @param msg The message to send
    * @param ref The actor ref to send to
    * @return a completed Route
    */
  def serviceAndComplete[T: ClassTag](msg: Any, ref: ActorRef)(implicit format: JsonFormat[T]): Route = {
    import ApiRoutesDefinition._
    val fut = service[T](msg, ref)
    //
    onComplete(fut) {
      // Case when response is a success.
      case util.Success(CompleteResult(t)) =>
        val resp = ApiResponse(ApiResponseMetaData(OK.intValue), Some(t))
        complete(resp)
      //Case when response is empty
      case util.Success(EmptyResult) =>
        complete((NotFound, NotFoundResp))
      //Case to handle validation miss
      case util.Success(fail: Failure) =>
        val status = fail.failType match {
          case FailureType.Validation => BadRequest
          case _ => InternalServerError
        }
        val apiResp = ApiResponse[String](ApiResponseMetaData(status.intValue, Some(fail.message)))
        complete((status, apiResp))
      //Case to handle service failure
      case util.Failure(ex) =>
        complete((InternalServerError, UnexpectedFailResp))
    }
  }

  /**
    * Uses ask to send a request to an actor, expecting a ServiceResult back in return
    *
    * @param msg The message to send
    * @param ref The actor ref to send to
    * @return a Future for a ServiceResult for type T
    */
  def service[T: ClassTag](msg: Any, ref: ActorRef) = {
    import akka.pattern.ask


    (ref ? msg).mapTo[ServiceResult[T]]
  }

}
