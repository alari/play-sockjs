package mirari.sockjs

import akka.actor.Actor
import play.api.mvc.RequestHeader
import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/13/13
 */
trait SockJsHandler extends Actor {
  def send(msg: JsValue) {
    context.parent ! Session.OutgoingMessage(msg)
  }
}

object SockJsHandler {

  sealed abstract class HandlerMessage

  case class Incoming(msg: JsValue) extends HandlerMessage

  case class Request(request: RequestHeader) extends HandlerMessage

  case class Outgoing(msg: JsValue) extends HandlerMessage


  private[sockjs] class Echo extends SockJsHandler {
    def receive = {
      case Incoming(msg) =>
        send(msg)
      case _ =>
    }
  }

  private[sockjs] class Closed extends SockJsHandler {
    var events = 0

    def receive = {
      case _ =>
        if (events > 0)
          context.parent ! Session.Close
        events += 1
    }
  }

}