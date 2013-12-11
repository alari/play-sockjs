package mirari.sockjs.handler

import akka.actor.Actor
import play.api.libs.json.JsValue
import mirari.sockjs.service.SockJsSession

/**
 * @author alari
 * @since 12/10/13
 */
abstract class Handler extends Actor {
  def receive = {
    case m: JsValue =>
      play.api.Logger.debug("--- HANDLE "+m)
      onMessage(m)
  }

  def onMessage(msg: JsValue)

  def send(msg: JsValue) {
    context.parent ! SockJsSession.Outgoing(msg)
  }
}

object Handler {

}