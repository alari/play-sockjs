package mirari.sockjs.impl

import akka.actor.{Props, Actor}
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
    case Register(service, handler, timeoutMs, heartbeatPeriodMs) =>
      context.child(service) match {
        case Some(s) =>
          sender ! s
        case None =>
          sender ! context.actorOf(Props(new Service(handler, timeoutMs, heartbeatPeriodMs)), service)
      }

    case SessionExists(service, id) =>
      context.child(service) match {
        case Some(s) =>
          val snd = sender
          s ? Service.SessionExists(id) pipeTo snd
        case None =>
          sender ! Failure(new Exception(s"Service $service not found"))
      }

    case RetrieveSession(service, id) =>
      context.child(service) match {
        case Some(s) =>
          val snd = sender
          s ? Service.RetrieveSession(id) pipeTo snd
        case None =>
          sender ! Failure(new Exception(s"Service $service not found"))
      }

    case CreateAndRetrieveSession(service, id) =>
      context.child(service) match {
        case Some(s) =>
          val snd = sender
          s ? Service.CreateAndRetrieveSession(id) pipeTo snd
        case None =>
          sender ! Failure(new Exception(s"Service $service not found"))
      }
  }
}

object Services {

  case class Register(service: String, handler: Props, timeoutMs: Int = SockJs.SessionTimeoutMs, heartbeatPeriodMs: Int = SockJs.SessionHeartbeatMs)

  case class SessionExists(service: String, id: String)

  case class RetrieveSession(service: String, id: String)

  case class CreateAndRetrieveSession(service: String, id: String)

}