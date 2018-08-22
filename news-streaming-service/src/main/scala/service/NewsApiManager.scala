package service

import akka.actor.ActorRef
import common._
import domain.Preference
import service.NewsApiManager.FetchHeadLines


object NewsApiManager {

  case class FetchHeadLines(preference: List[Preference])


}

/** Created by AYON SANYAL on 28-05-2018.
  * This is a manager for this project whose job is to
  * delegate the  incoming request to its child actors .
  *
  */
class NewsApiManager extends ApiServiceManager {

  override def receive = standardMessageHandlingForFetchingNews


  def standardMessageHandlingForFetchingNews: Receive = {
    //The request from web service
    case fetchHeadLines: FetchHeadLines => {
      newsCollectionService(sender) ! fetchHeadLines

    }
    //Case where unknown message type comes.
    case _ => sender ! NewsCollectionService.validationErrorResponse

  }

  def newsCollectionService(caller: ActorRef) = context.actorOf(NewsCollectionService.props(caller))
}
