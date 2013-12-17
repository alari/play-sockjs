package mirari.sockjs.transport

import play.api.mvc.{RequestHeader, Action}
import akka.actor.ActorRef
import play.api.libs.concurrent.Execution.Implicits._
import mirari.sockjs.{SockJsService, Frames}

/**
 * @author alari
 * @since 12/16/13
 */
private[transport] trait EventSourceTransport {
  self: SockJsController with SockJsService =>

  private def eventsourceTransport(session: ActorRef)(implicit request: RequestHeader) =
    Transport.halfDuplex(session, Frames.Prelude.eventsource, Frames.Format.eventsource, maxBytesSent).map {
      _.out
    }

  private[sockjs] def eventsource(session: String) = Action.async {
    implicit request =>
      createSession(session).flatMap {
        s =>
          eventsourceTransport(s).map {
            out =>
              Ok.chunked(out).withHeaders(
                CONTENT_TYPE -> "text/event-stream;charset=UTF-8",
                CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*).withCookies(cookies: _*)
          }
      }
  }
}
