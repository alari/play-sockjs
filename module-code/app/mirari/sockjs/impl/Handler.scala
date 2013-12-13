package mirari.sockjs.impl

import play.api.libs.json.JsValue
import akka.actor.Actor
import play.api.mvc.RequestHeader

/**
 * @author alari
 * @since 12/13/13
 */
class Handler extends Actor {
  import Handler._

  def receive = {
    case request: RequestHeader =>

    case Incoming(msg) =>
      play.api.Logger.debug("Echoing message back: "+msg)
      send(msg)
  }

  def send(msg: JsValue) {
    context.parent ! Session.OutgoingMessage(msg)
  }
}

object Handler {
  case class Incoming(msg: JsValue)
}