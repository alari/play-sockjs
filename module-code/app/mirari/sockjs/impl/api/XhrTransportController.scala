package mirari.sockjs.impl.api

import akka.actor.ActorRef
import play.api.mvc.Action
import mirari.sockjs.impl.{Session, SockJsTransports}
import mirari.sockjs.frames.JsonCodec
import com.fasterxml.jackson.core.JsonParseException
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json

/**
 * @author alari
 * @since 12/16/13
 */
object XhrTransportController extends SockJsController with SockJsTransports {
  def opts = CORSOptions("OPTIONS", "POST")

  def xhrPolling(service: ActorRef, session: String) = Action.async {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          xhrPollingTransport(s).map(Ok(_).withHeaders(
            CONTENT_TYPE -> "application/javascript;charset=UTF-8",
            CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0"
          ).withHeaders(cors: _*))
      }
  }

  val maxLength = 4096

  def xhrSend(service: ActorRef, session: String) = Action.async(parse.anyContent) {
    implicit request =>
      getSession(service, session).map {
        case Some(s) =>
          val message: String = request.body.asRaw.flatMap(r ⇒ r.asBytes(maxLength).map(b ⇒ new String(b)))
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
                .withHeaders(cors: _*)
            } catch {
              case e: JsonParseException ⇒
                InternalServerError("Broken JSON encoding.")
            }
        case None =>
          NotFound
      }
  }

  def xhrStream(service: ActorRef, session: String) = Action.async {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          xhrStreamingTransport(s, maxLength).map {
            out =>
              Ok.chunked(out).withHeaders(
                CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0"
              ).withHeaders(cors: _*)
          }
      }
  }
}
