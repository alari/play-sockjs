package mirari.sockjs.impl.api

import play.api.mvc._
import java.text.SimpleDateFormat
import java.util.Date
import akka.actor.ActorRef
import mirari.sockjs.impl.{Service, SockJs}

/**
 * @author alari
 * @since 12/16/13
 */
trait SockJsController extends Controller {
  def CORSOptions(methods: String*) = Action {
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

  def cors(implicit req: RequestHeader) = Seq(
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    ACCESS_CONTROL_ALLOW_ORIGIN -> req.headers.get("origin").map(o => if (o != "null") o else "*").getOrElse("*"))
    .union(
      (for (acrh ← req.headers.get(ACCESS_CONTROL_REQUEST_HEADERS))
      yield ACCESS_CONTROL_ALLOW_HEADERS -> acrh).toSeq)

  import SockJs.Timeout
  import akka.pattern.ask

  def getSession(service: ActorRef, id: String) = (service ? Service.RetrieveSession(id)).mapTo[Option[ActorRef]]

  def createSession(service: ActorRef, id: String) = (service ? Service.CreateAndRetrieveSession(id)).mapTo[ActorRef]
}
