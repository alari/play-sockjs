package mirari.sockjs.impl.api

import akka.actor.ActorRef
import play.api.mvc._
import mirari.sockjs.impl.{Session, SockJsTransports}
import scala.concurrent.Future
import mirari.sockjs.frames.JsonCodec
import com.fasterxml.jackson.core.JsonParseException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.Some
import play.api.mvc.SimpleResult

/**
 * @author alari
 * @since 12/16/13
 */
object JsonpTransportController extends SockJsController with SockJsTransports {
  def jsonp(service: ActorRef, session: String) = Action.async {
    implicit request =>
      createSession(service, session).flatMap {
        s =>
          request.getQueryString("c").map {
            callback =>
              jsonpTransport(s, callback).map {
                Ok(_).withHeaders(cors: _*).withHeaders(
                  CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0").withCookies(cookies: _*)
              }
          }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required\n")))
      }
  }

  def jsonpSend(service: ActorRef, session: String) = Action.async(parse.anyContent) {
    implicit request =>
      getSession(service, session).map {
        case Some(s) =>
          jsonpResult {
            message =>
              try {
                s ! Session.IncomingJson(JsonCodec.decodeJson(message))
                Ok("ok")
                  .withHeaders(
                    CONTENT_TYPE -> "text/plain; charset=UTF-8",
                    CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                  .withHeaders(cors: _*).withCookies(cookies: _*)
              } catch {
                case e: JsonParseException => InternalServerError("Broken JSON encoding.")
              }
          }
        case None =>
          NotFound
      }
  }

  private def jsonpResult(f: String => SimpleResult)(implicit request: Request[AnyContent]): SimpleResult =
    if (request.contentType.map(_.toLowerCase).exists(ct => ct.startsWith("application/x-www-form-urlencoded") || ct.startsWith("text/plain")))
      jsonpBody.map(body => f(body))
        .getOrElse(InternalServerError("Payload expected."))
    else InternalServerError("Invalid Content-Type")

  private def jsonpBody(implicit request: Request[AnyContent]): Option[String] =
    request.body.asFormUrlEncoded.flatMap(formBody => formBody.get("d").map(seq => seq.reduceLeft(_ + _)))
      .orElse(request.body.asText
      ).filter(_ != "")
}
