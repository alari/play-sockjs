package infra.sockjs.transport

import akka.actor.ActorRef
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import com.fasterxml.jackson.core.JsonParseException
import play.api.mvc.Result
import infra.sockjs.{JsonCodec, Frames}
import infra.sockjs
import infra.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
private[transport] trait JsonPTransport {
  self: SockJsController with SockJsService =>

  private def jsonpTransport(session: ActorRef, callback: String)(implicit request: RequestHeader, ctx: ExecutionContext) =
    Transport.singleFramePlex(session).flatMap {
      _.out.map(Frames.Format.jsonp(callback))
    }

  import play.api.libs.concurrent.Execution.Implicits._


  private[sockjs] def jsonp(session: String) = Action.async {
    implicit request =>
      createSession(session).flatMap {
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

  private[sockjs] def jsonpSend(session: String) = Action.async(parse.anyContent) {
    implicit request =>
      getSession(session).map {
        case Some(s) =>
          jsonpResult {
            message =>
              try {
                s ! sockjs.Session.IncomingJson(JsonCodec.decodeJson(message))
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

  private def jsonpResult(f: String => Result)(implicit request: Request[AnyContent]): Result =
    if (request.contentType.map(_.toLowerCase).exists(ct => ct.startsWith("application/x-www-form-urlencoded") || ct.startsWith("text/plain")))
      jsonpBody.map(body => f(body))
        .getOrElse(InternalServerError("Payload expected."))
    else InternalServerError("Invalid Content-Type")

  private def jsonpBody(implicit request: Request[AnyContent]): Option[String] =
    request.body.asFormUrlEncoded.flatMap(formBody => formBody.get("d").map(seq => seq.reduceLeft(_ + _)))
      .orElse(request.body.asText
      ).filter(_ != "")
}
