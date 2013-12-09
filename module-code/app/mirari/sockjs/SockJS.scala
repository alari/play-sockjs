package mirari.sockjs

import play.api.libs.json.Json
import akka.actor.{ActorSystem, Props, Actor}
import akka.pattern.{ask, pipe}

/**
 * @author alari
 * @since 12/9/13
 */
class SockJS extends Actor {

  import SockJS._
  implicit val Timeout = akka.util.Timeout(100)
  implicit val ctx = SockJS.ctx

  def receive = {
    case RegisterService(name, props) =>
      context.actorOf(props, name)

    case ServiceAsk(service, message) =>
      val s = sender

      context.child(service) match {
        case Some(a) => a ? message pipeTo s
        case _ => s ! ServiceNotFound
      }


    case ServiceMessage(service, message) =>

      context.child(service) match {
        case Some(a) => a ! message
        case _ => play.api.Logger.error(s"SockJS service not found: $service")
      }

  }
}

object SockJS {

  implicit val Timeout = akka.util.Timeout(100)

  lazy val system = ActorSystem("sockjs")

  implicit val ctx = system.dispatcher

  lazy val actor = system.actorOf(Props[SockJS], "router")

  def registerService(name: String, props: Props) = actor ! RegisterService(name, props)
  
  def message(service: String, message: Any) = actor ! ServiceMessage(service, message)

  def askService(service: String, message: Any) =
    actor ? ServiceAsk(service, message)

  case class RegisterService(name: String, props: Props)
  
  case class ServiceMessage(service: String, msg: Any)

  case class ServiceAsk(service: String, msg: Any)

  case object InfoRequest

  case class Info(websocket: Boolean, cookie_needed: Boolean, origins: Seq[String] = Seq("*:*"), entropy: Long = (Math.random()*100000).toLong)

  object Info {
    implicit val writes = Json.writes[Info]
  }

  case object ServiceNotFound

}


class EchoService extends Actor {

  import SockJS._

  def receive = {
    case InfoRequest =>
      sender ! Info(
        websocket = true,
        cookie_needed = false
      )
  }
}

class DisabledWebsocketEchoService extends Actor {
  import SockJS._

  def receive = {
    case InfoRequest =>
    sender ! Info(
    websocket = false,
    cookie_needed = false
    )
  }
}