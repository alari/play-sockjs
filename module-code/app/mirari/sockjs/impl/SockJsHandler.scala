package mirari.sockjs.impl

import play.api.libs.json.JsValue
import akka.actor.Actor
import play.api.mvc.RequestHeader

/**
 * @author alari
 * @since 12/13/13
 */
trait SockJsHandler extends Actor {

  import SockJsHandler._

  def receive = {
    case request: RequestHeader =>

    case Incoming(msg) =>
  }

  def send(msg: JsValue) {
    context.parent ! Session.OutgoingMessage(msg)
  }
}

object SockJsHandler {

  case class Incoming(msg: JsValue)

  class Echo extends SockJsHandler {
    override def receive = {
      case Incoming(msg) =>
        send(msg)
    }
  }

  class Closed extends SockJsHandler {
    var events = 0
    override def receive = {
      case _ =>
        if(events > 0)
          context.parent ! Session.Close
      events += 1
    }
  }

}