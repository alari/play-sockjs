package mirari.sockjs.transport

import akka.actor.ActorRef
import play.api.mvc.{RequestHeader, Action}
import com.fasterxml.jackson.core.JsonParseException
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import mirari.sockjs.{JsonCodec, Session, Frames, SockJsService}

/**
 * @author alari
 * @since 12/16/13
 */
private[transport] trait XhrTransport {
  self: SockJsService with SockJsController =>

  import Transport._

  private def xhrPollingTransport(session: ActorRef)(implicit request: RequestHeader, ctx: ExecutionContext) =
    singleFramePlex(session).flatMap {
      _.out.map(Frames.Format.xhr)
    }

  private def xhrStreamingTransport(session: ActorRef, maxBytesSent: Int = maxBytesSent)(implicit request: RequestHeader) =
    halfDuplex(session, Frames.Prelude.xhrStreaming, Frames.Format.xhr, maxBytesSent).map {
      _.out
    }

  private[sockjs] def xhrOptions = CORSOptions("OPTIONS", "POST")

  private[sockjs] def xhrPolling(session: String) = Action.async {
    implicit request =>
      createSession(session).flatMap {
        s =>
          xhrPollingTransport(s).map(Ok(_).withHeaders(
            CONTENT_TYPE -> "application/javascript;charset=UTF-8",
            CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0"
          ).withHeaders(cors: _*).withCookies(cookies: _*))
      }
  }


  private[sockjs] def xhrSend(session: String) = Action.async(parse.anyContent) {
    implicit request =>
      getSession(session).map {
        case Some(s) =>
          val message: String = request.body.asRaw.flatMap(r ⇒ r.asBytes(maxBytesReceived).map(b ⇒ new String(b)))
            .getOrElse(request.body.asText
            .orElse(request.body.asJson map Json.stringify)
            .getOrElse(""))
          if (message == "") {
            play.api.Logger.error(s"xhr_send error: couldn't read the body, content-type: ${request.contentType}")
            InternalServerError("Payload expected.")
          } else
            try {
              s ! Session.IncomingJson(JsonCodec.decodeJson(message))
              NoContent
                .withHeaders(
                  CONTENT_TYPE -> "text/plain; charset=UTF-8",
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*).withCookies(cookies: _*)
            } catch {
              case e: JsonParseException ⇒
                InternalServerError("Broken JSON encoding.")
            }
        case None =>
          NotFound
      }
  }

  private[sockjs] def xhrStream(session: String) = Action.async {
    implicit request =>
      createSession(session).flatMap {
        s =>
          xhrStreamingTransport(s, maxBytesSent).map {
            out =>
              Ok.chunked(out).withHeaders(
                CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0"
              ).withHeaders(cors: _*).withCookies(cookies: _*)
          }
      }
  }
}
