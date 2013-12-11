package mirari.sockjs.transport

import play.api.mvc.Action
import play.api.libs.iteratee.Concurrent
import mirari.sockjs.frames.StringEscapeUtils
import scala.concurrent.Future
import mirari.sockjs.service.SockJsSession
import akka.actor.{ActorRef, Props}
import concurrent.ExecutionContext.Implicits.global


/**
 * @author alari
 * @since 12/11/13
 */
class HtmlFileTransport(callback: String, channel: Concurrent.Channel[String], maxBytesStreaming: Int) extends TransportActor {
  var bytesSent = 0

  override def doRegister() {
    channel push s"""
    <!doctype html>
<html><head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head><body><h2>Don't panic!</h2>
  <script>
    document.domain = document.domain;
    var c = parent.$callback;
    c.start();
    function p(d) {c.message(d);};
    window.onload = function() {c.stop();};
  </script>
    """
    super.doRegister()
  }

  def sendFrame(m: String): Boolean = {

    val msg = "<script>\np(\"" + StringEscapeUtils.escapeJavaScript(m) + "\");\r\n</script>"

    bytesSent += msg.length
    println("HtmlFile ::<<<<<<<<< " + m)
    channel push msg
    if (bytesSent < maxBytesStreaming)
      true
    else {
      channel.eofAndEnd()
      false
    }
  }
}

object HtmlFileController extends TransportController {

  import akka.pattern.ask

  def htmlfile(service: String, server: String, session: String) = Action.async {
    implicit request =>

      withExistingSessionFlat(service, session) {
        ss =>
          request.getQueryString("c").map {
            callback =>
              val (enum, channel) = Concurrent.broadcast[String]
              ss ? SockJsSession.CreateAndRegister(Props(new HtmlFileTransport(callback, channel, 12700)), "htmlfile") map {
                case transport: ActorRef =>
                  Ok.chunked(enum).as("text/html; charset=UTF-8")
                    .withHeaders(CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                    .withHeaders(cors: _*)
                case _ =>
                  InternalServerError
              }

          }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required\n")))
      }
  }
}
