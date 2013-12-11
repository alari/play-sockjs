package mirari.sockjs.service

import akka.actor.{Actor, Props}
import mirari.sockjs.SockJsSystem

/**
 * @author alari
 * @since 12/10/13
 */
class SockJsService(handler: Props, websocket: Boolean, cookieNeeded: Boolean) extends Actor {

  import SockJsService._

  def receive = {
    case SockJsSystem.RequestInfo =>
      sender ! SockJsSystem.Info(websocket, cookieNeeded)

    case GetSession(id) =>
      context.child(id) match {
        case Some(a) => sender ! a
        case None => sender ! SessionNotFound
      }

    case GetOrCreateSession(id) =>
      context.child(id) match {
        case Some(a) => sender ! a
        case None =>
          sender ! context.actorOf(Props(new SockJsSession(handler)), id)
      }
  }
}

object SockJsService {

  case class GetSession(id: String)

  case class GetOrCreateSession(id: String)

  object SessionNotFound

}