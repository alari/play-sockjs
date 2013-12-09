package mirari.sockjs

import play.api.mvc._
import play.api.libs.json.Json
import org.joda.time.DateTime

object Application extends Controller {

  import SockJS._

  val ETagValue = "asdasdasdasd"

  val iframeBody =
    """
      |<!DOCTYPE html>
      |<html>
      |<head>
      |  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      |  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
      |  <script>
      |    document.domain = document.domain;
      |    _sockjs_onload = function(){SockJS.bootstrap_iframe();};
      |  </script>
      |  <script src="https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.min.js"></script>
      |</head>
      |<body>
      |  <h2>Don't panic!</h2>
      |  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>
      |</body>
      |</html>
    """.stripMargin

  def index(service: String) = Action {
    Ok("Welcome to SockJS!\n").withHeaders("content-type" -> "text/plain; charset=UTF-8").withCookies(Seq(): _*)
  }

  def indexNoSlash(service: String) = index(service)

  def iframe(service: String, any: String) = Action {
    request =>
      request.headers.get(IF_NONE_MATCH) match {
        case Some(e) if e == ETagValue =>
          NotModified
        case _ =>
          Ok(iframeBody).withHeaders(
            CONTENT_TYPE -> "text/html; charset=UTF-8",
            ETAG -> ETagValue,
            "Cache-Control" -> "public; max-age=31536000",
          EXPIRES -> DateTime.now().plusYears(1).toString
          )
      }
  }

  def infoOpts(service: String) = Action {
    request =>
      NoContent.withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers.get(ORIGIN).filter(_ != "null").getOrElse("*"),
        ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
      ACCESS_CONTROL_ALLOW_METHODS -> "OPTIONS, GET",
        "Cache-Control" -> "public; max-age=31536000",
        EXPIRES -> DateTime.now().plusYears(1).toString,
      "access-control-max-age" -> "1000001"
      )
  }

  def info(service: String) = Action.async {
    request =>
      SockJS.askService(service, InfoRequest).map {
        case i: Info => Ok(Json.toJson(i)).withHeaders(
          CONTENT_TYPE -> "application/json; charset=UTF-8",
          "Cache-Control" -> "no-store, no-cache, must-revalidate, max-age=0",
          ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers.get(ORIGIN).filter(_ != "null").getOrElse("*"),
          ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
        )
        case ServiceNotFound => NotFound
      }
  }

  def chunking_testOpts(service: String) = Action {
    NotImplemented
  }

  def chunking_test(service: String) = Action {
    NotImplemented
  }

  def rootWebsocket(service: String) = Action {
    NotImplemented
  }

  def jsonp(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def jsonp_send(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhrOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_send(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_sendOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streaming(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streamingOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def eventsource(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def htmlfile(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def websocket(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def notFound(any: String) = Action {
    request =>
      play.api.Logger.error(request.uri)
      NotFound
  }

}