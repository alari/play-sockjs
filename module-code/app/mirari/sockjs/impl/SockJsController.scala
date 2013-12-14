package mirari.sockjs.impl

import play.api.mvc.{RequestHeader, Controller}
import scala.util.Random
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 2:30
 */
class SockJsController extends Controller with SockJsTransports {
  def randomNumber() = 2L << 30 + Random.nextInt

  def handleCORSOptions(methods: String*)(implicit request: RequestHeader) = {
    val oneYearSeconds = 365 * 24 * 60 * 60
    val oneYearms = oneYearSeconds * 1000
    val expires = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
      .format(new Date(System.currentTimeMillis() + oneYearms))
    NoContent
      .withHeaders(
        EXPIRES -> expires,
        CACHE_CONTROL -> "public,max-age=31536000",
        ACCESS_CONTROL_ALLOW_METHODS -> methods.reduceLeft(_ + ", " + _),
        ACCESS_CONTROL_MAX_AGE -> oneYearSeconds.toString)
      .withHeaders(cors: _*)
  }

  def cors(implicit req: RequestHeader) = Seq(
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    ACCESS_CONTROL_ALLOW_ORIGIN -> req.headers.get("origin").map(o => if (o != "null") o else "*").getOrElse("*"))
    .union(
      (for (acrh ← req.headers.get(ACCESS_CONTROL_REQUEST_HEADERS))
      yield ACCESS_CONTROL_ALLOW_HEADERS -> acrh).toSeq)


}
