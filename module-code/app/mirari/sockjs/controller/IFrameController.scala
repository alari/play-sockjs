package mirari.sockjs.controller

import play.api.mvc._
import java.security.MessageDigest
import org.joda.time.DateTime
import play.api.Play.current
import org.apache.commons.lang3.StringUtils

/**
 * @author alari
 * @since 12/10/13
 */
object IFrameController extends Controller{


  class IframePage(clientUrl: String) {

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
</html>""".replaceAll("""(?m)\s+$""", "")

    lazy val etag: String = {
      import org.apache.commons.codec.binary.Base64

      new String(new Base64().encode(MessageDigest.getInstance("SHA").digest(content.getBytes)))
    }

  }

  val iframePage = new IframePage(play.api.Play.configuration.getString("mirari.sockjs.clientUrl").getOrElse("https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.min.js"))

  def iframe(service: String, any: String) = Action {
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

  def index(service: String) = Action {
    Ok("Welcome to SockJS!\n").withHeaders("content-type" -> "text/plain; charset=UTF-8").withCookies(Seq(): _*)
  }

  def indexNoSlash(service: String) = index(service)
}
