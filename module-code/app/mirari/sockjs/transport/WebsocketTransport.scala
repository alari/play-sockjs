package mirari.sockjs.transport

import akka.actor.ActorRef
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author alari
 * @since 12/16/13
 */
private[transport] trait WebsocketTransport {
  self: SockJsController =>

  private def websocketTransport(session: ActorRef)(implicit request: RequestHeader) =
    Transport.fullDuplex(session).map {
      t =>
        (t.in, t.out)
    }

  private[sockjs] def websocket(session: String) = WebSocket.async[String] {
    implicit request =>
      createSession(session).flatMap {
        s =>
          websocketTransport(s)
      }
  }
}
