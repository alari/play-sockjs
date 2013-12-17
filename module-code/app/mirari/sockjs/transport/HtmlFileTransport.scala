package mirari.sockjs.transport

import play.api.mvc.{RequestHeader, Action}
import akka.actor.ActorRef
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import mirari.sockjs.{Frames, SockJsService}

/**
 * @author alari
 * @since 12/16/13
 */
private[transport] trait HtmlFileTransport {
  self: SockJsController with SockJsService =>

  private def htmlfileTransport(session: ActorRef, callback: String)(implicit request: RequestHeader) =
    Transport.halfDuplex(session, Frames.Prelude.htmlfile(callback), Frames.Format.htmlfile, maxBytesSent).map {
      _.out
    }

  private[sockjs] def htmlfile(session: String) = Action.async {
    implicit request =>
      createSession(session).flatMap {
        s =>
          request.getQueryString("c").map {
            callback =>
              htmlfileTransport(s, callback).map {
                out =>
                  Ok.chunked(out).withHeaders(
                    CONTENT_TYPE -> "text/html;charset=UTF-8",
                    CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                    .withHeaders(cors: _*).withCookies(cookies: _*)
              }
          }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required\n")))
      }
  }
}
