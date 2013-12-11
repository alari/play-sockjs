package mirari.sockjs.handler

import akka.actor.Actor
import play.api.libs.json.JsValue
import mirari.sockjs.service.SockJsSession
import play.api.mvc.RequestHeader

/**
 * @author alari
 * @since 12/10/13
 */
abstract class SockJsHandler extends Actor {
  def receive = {
    case r: RequestHeader =>
      onRequest(r)

    case m: JsValue =>
      play.api.Logger.debug("--- HANDLE "+m)
      onMessage(m)
  }

  def onRequest(request: RequestHeader) {
    play.api.Logger.debug("request: "+request)
  }

  def onMessage(msg: JsValue)

  def send(msg: JsValue) {
    context.parent ! SockJsSession.Outgoing(msg)
  }
}