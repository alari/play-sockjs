package mirari.sockjs.transport

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.actor.{ActorRef, Props}
import mirari.sockjs.frames.{JsonCodec, StringEscapeUtils}
import StringEscapeUtils._
import mirari.sockjs.service.SockJsSession
import java.net.URLDecoder
import com.fasterxml.jackson.core.JsonParseException
import ExecutionContext.Implicits.global

/**
 * @author alari
 * @since 12/11/13
 */
class JsonpTransport(promise: Promise[String]) extends TransportActor {
  def sendFrame(msg: String): Boolean = {
    println("JSONP <<<<<<<<K: " + msg)
    promise success msg
    false
  }
}

object JsonpController extends TransportController {

  import akka.pattern.ask

  def jsonp(service: String, server: String, session: String) = Action.async {
    implicit request =>

      withExistingSessionFlat(service, session) {
        ss =>
          request.getQueryString("c").map {
            callback =>
              val promise = Promise[String]()
              ss ? SockJsSession.CreateAndRegister(Props(new JsonpTransport(promise)), "jsonp", request) flatMap {
                case transport: ActorRef =>
                  promise.future.map(m =>
                    Ok( s"""${callback}("${escapeJavaScript(m)}");\r\n""") // callback(\\"m\\");\r\n
                      .withHeaders(
                        CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                        CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                      .withHeaders(cors: _*))
                case _ =>
                  Future.successful(InternalServerError)
              }

          }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required\n")))
      }
  }

  def jsonp_send(service: String, server: String, session: String) = Action.async(parse.anyContent) {
    implicit request =>
      withSession(service, session) {
        ss =>
          jsonpResult {
            message =>
              try {
                ss ! SockJsSession.Incoming(JsonCodec.decodeJson(message))
                println(s"JsonPSend :: ___>>>>>--" + JsonCodec.decodeJson(message))
                Ok("ok")
                  .withHeaders(
                    CONTENT_TYPE -> "text/plain; charset=UTF-8",
                    CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                  .withHeaders(cors: _*)
              } catch {
                case e: JsonParseException => InternalServerError("Broken JSON encoding.")
              }
          }
      }
  }

  def jsonpResult(f: String => SimpleResult)(implicit request: Request[AnyContent]): SimpleResult =
    if (request.contentType.map(_.toLowerCase).exists(ct => ct.startsWith("application/x-www-form-urlencoded") || ct.startsWith("text/plain")))
      jsonpBody.map(body => f(body))
        .getOrElse(InternalServerError("Payload expected."))
    else InternalServerError("Invalid Content-Type")

  def jsonpBody(implicit request: Request[AnyContent]): Option[String] =
    request.body.asFormUrlEncoded.flatMap(formBody => formBody.get("d").map(seq => seq.reduceLeft(_ + _)))
      .orElse(request.body.asText
      .map(textBody => URLDecoder.decode(textBody, "UTF-8"))
      .filter(decodedBody => decodedBody.size > 2 && decodedBody.startsWith("d="))
      .map(decodedBody => decodedBody.substring(2)))
}