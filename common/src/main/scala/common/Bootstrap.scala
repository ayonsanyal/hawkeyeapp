package common

import akka.actor.ActorSystem

/**
  * Created by AYON SANYAL on 28-05-2018.
  */
trait Bootstrap {

  /**
    * It boots up the actors for a service module and returns the service endpoints for that
    * module to be included in the Unfiltered server as plans
    *
    * @param system The actor system to boot actors into
    * @return a List of Movie Services to be added into server
    */
  def bootup(system: ActorSystem): List[ApiRouteDefinition]
}
