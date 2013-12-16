package mirari.sockjs.impl.api

import mirari.sockjs.impl.SockJsTransports
import play.api.mvc.Action
import akka.actor.ActorRef
import concurrent.ExecutionContext.Implicits.global

/**
 * @author alari
 * @since 12/16/13
 */
object EventsourceTransportController extends SockJsController with SockJsTransports{
  val maxBytesSent = 4096

  def eventsource(service: ActorRef, session: String) = Action.async {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          eventsourceTransport(s, maxBytesSent).map {
            out =>
              Ok.chunked(out).withHeaders(
                CONTENT_TYPE -> "text/event-stream;charset=UTF-8",
                CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*)
          }
      }
  }
}
