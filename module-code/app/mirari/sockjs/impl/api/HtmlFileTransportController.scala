package mirari.sockjs.impl.api

import mirari.sockjs.impl.SockJsTransports
import play.api.mvc.Action
import akka.actor.ActorRef
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author alari
 * @since 12/16/13
 */
object HtmlFileTransportController extends SockJsController with SockJsTransports {
  val maxBytesSent = 4096

  def htmlfile(service: ActorRef, session: String) = Action.async {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          request.getQueryString("c").map {
            callback =>
              htmlfileTransport(s, callback, maxBytesSent).map {
                out =>
                  Ok.chunked(out).withHeaders(
                    CONTENT_TYPE -> "text/html;charset=UTF-8",
                    CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                    .withHeaders(cors: _*)
              }
          }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required\n")))
      }
  }
}
