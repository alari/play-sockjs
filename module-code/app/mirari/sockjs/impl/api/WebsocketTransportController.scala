package mirari.sockjs.impl.api

import akka.actor.ActorRef
import play.api.mvc._
import mirari.sockjs.impl.SockJsTransports
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author alari
 * @since 12/16/13
 */
object WebsocketTransportController extends SockJsController with SockJsTransports {
  def websocket(service: ActorRef, session: String) = WebSocket.async[String] {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          websocketTransport(s)
      }
  }
}
