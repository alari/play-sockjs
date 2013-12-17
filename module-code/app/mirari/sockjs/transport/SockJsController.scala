package mirari.sockjs.transport

import play.api.mvc._
import java.text.SimpleDateFormat
import java.util.Date
import akka.actor.ActorRef
import mirari.sockjs.{SockJsService, Service, SockJs}

/**
 * @author alari
 * @since 12/16/13
 */
trait SockJsController extends Controller
with XhrTransport
with EventSourceTransport
with HtmlFileTransport
with JsonPTransport
with StaticController
with WebsocketTransport {
  self: SockJsService =>

  private[sockjs] def CORSOptions(methods: String*) = Action {
    implicit request =>

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

  protected def cors(implicit req: RequestHeader) = Seq(
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    ACCESS_CONTROL_ALLOW_ORIGIN -> req.headers.get("origin").map(o => if (o != "null") o else "*").getOrElse("*"))
    .union(
      (for (acrh ← req.headers.get(ACCESS_CONTROL_REQUEST_HEADERS))
      yield ACCESS_CONTROL_ALLOW_HEADERS -> acrh).toSeq)

  private[sockjs] def cookies(implicit req: RequestHeader) = Seq(
    req.cookies.get("JSESSIONID").getOrElse(Cookie("JSESSIONID", "dummy"))
  )

  import SockJs.Timeout
  import akka.pattern.ask

  private[sockjs] def getSession(id: String) = (service ? Service.RetrieveSession(id)).mapTo[Option[ActorRef]]

  private[sockjs] def createSession(id: String) = (service ? Service.CreateAndRetrieveSession(id)).mapTo[ActorRef]
}
