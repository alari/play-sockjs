package mirari.sockjs

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.{ask, pipe}
import akka.actor.Status.Failure

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 1:59
 */
class Services extends Actor {

  import Services._
  import SockJs.Timeout
  import context.dispatcher

  def receive = {
    case Register(service, params) =>
      context.child(service) match {
        case Some(s) =>
          sender ! s
        case None =>
          sender ! context.actorOf(Props(new Service(params)), service)
      }

    case SessionExists(service, id) =>
      fromService(service, Service.SessionExists(id))

    case RetrieveSession(service, id) =>
      fromService(service, Service.RetrieveSession(id))

    case CreateAndRetrieveSession(service, id) =>
      fromService(service, Service.CreateAndRetrieveSession(id))
  }

  def withService(service: String)(f: ActorRef => Unit) {
    context.child(service) match {
      case Some(s) =>
        f(s)
      case None =>
        sender ! Failure(new Exception(s"Service $service not found"))
    }
  }

  def fromService(service: String, message: Any) {
    withService(service) {
      s =>
        val snd = sender
        s ? message pipeTo snd
    }
  }
}

object Services {

  case class Register(service: String, params: Service.Params)

  case class SessionExists(service: String, id: String)

  case class RetrieveSession(service: String, id: String)

  case class CreateAndRetrieveSession(service: String, id: String)

  case class GetInfo(service: String)
}