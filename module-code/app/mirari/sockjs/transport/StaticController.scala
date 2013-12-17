package mirari.sockjs.transport

import play.api.mvc.{WebSocket, Action}
import play.api.libs.json.Json
import scala.util.Random
import java.security.MessageDigest
import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.Some
import mirari.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
trait StaticController {
  self: SockJsController with SockJsService =>

  private[sockjs] def infoOpts = CORSOptions("OPTIONS", "GET")

  private def randomNumber() = 2L << 30 + Random.nextInt

  private[sockjs] def info = Action {
    implicit request =>
      Ok(Json.obj(
        "websocket" -> websocketEnabled,
        "cookie_needed" -> cookieNeeded,
        "origins" -> Seq("*:*"),
        "entropy" -> randomNumber()
      )).withHeaders(
          CONTENT_TYPE -> "application/json; charset=UTF-8",
          CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0"
        ).withHeaders(cors: _*)
  }

  private[sockjs] def greeting = Action {
    Ok("Welcome to SockJS!\n").withHeaders("content-type" -> "text/plain; charset=UTF-8").withCookies(Seq(): _*)
  }

  private class IframePage(clientUrl: String) {

    val content =
      s"""<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <script>
    document.domain = document.domain;
    _sockjs_onload = function(){SockJS.bootstrap_iframe();};
  </script>
  <script src="$clientUrl"></script>
</head>
<body>
  <h2>Don't panic!</h2>
  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>
</body>
</html>""".replaceAll( """(?m)\s+$""", "")

    lazy val etag: String = {
      import org.apache.commons.codec.binary.Base64

      new String(new Base64().encode(MessageDigest.getInstance("SHA").digest(content.getBytes)))
    }

  }

  private val iframePage = new IframePage(clientScriptSrc)

  private[sockjs] def iframe = Action {
    request =>
      request.headers.get(IF_NONE_MATCH) match {
        case Some(e) if e == iframePage.etag =>
          NotModified
        case _ =>
          Ok(iframePage.content).withHeaders(
            CONTENT_TYPE -> "text/html; charset=UTF-8",
            ETAG -> iframePage.etag,
            CACHE_CONTROL -> "public; max-age=31536000",
            EXPIRES -> DateTime.now().plusYears(1).toString
          )
      }
  }

  private[sockjs] def rawWebsocket = WebSocket.async[String] {
    request =>
      import ExecutionContext.Implicits.global

      val p = Promise[Concurrent.Channel[String]]()
      val f = p.future

      Future.successful((Iteratee.foreach[String](c => f.map(_.push(c))), Concurrent.unicast[String](onStart = {
        c => p.success(c)
      })))
  }
}
