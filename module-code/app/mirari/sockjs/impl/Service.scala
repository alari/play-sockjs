package mirari.sockjs.impl

import akka.actor.{Props, Actor}

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 1:50
 */
class Service(handlerProps: Props, timeoutMs: Int = SockJs.SessionTimeoutMs, heartbeatPeriodMs: Int = SockJs.SessionHeartbeatMs) extends Actor {

  import Service._

  def receive = {
    case SessionExists(id) =>
      sender ! context.child(id).isDefined

    case RetrieveSession(id) =>
      sender ! context.child(id)

    case CreateAndRetrieveSession(id) =>
      context.child(id) match {
        case Some(s) => sender ! s
        case None =>
          sender ! context.actorOf(Props(new Session(handlerProps, timeoutMs = timeoutMs, heartbeatPeriodMs = heartbeatPeriodMs)), id)
      }
  }
}

object Service {

  case class RetrieveSession(id: String)

  case class CreateAndRetrieveSession(id: String)

  case class SessionExists(id: String)

}
