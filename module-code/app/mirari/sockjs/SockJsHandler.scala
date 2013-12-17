package mirari.sockjs

import akka.actor.Actor
import play.api.mvc.RequestHeader
import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/13/13
 */
trait SockJsHandler extends Actor {


  def receive = {
    case request: RequestHeader =>

    case SockJsHandler.Incoming(msg) =>
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