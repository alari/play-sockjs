package mirari.sockjs.transport

import akka.actor._
import akka.event.Logging
import mirari.sockjs.service.{SockJsService, SockJsSession}
import mirari.sockjs.frames.SockJsFrames
import scala.concurrent.duration.DurationInt
import play.api.mvc.{SimpleResult, AnyContent, Request, Controller}
import scala.util.Random
import scala.concurrent.{Promise, Future}
import mirari.sockjs.SockJsSystem

/**
 * @author alari
 * @since 12/10/13
 */
abstract class TransportActor extends Actor {
  import scala.language.postfixOps
  implicit val executionContext = context.system.dispatcher

  val logger = Logging(context.system, this)
  val heartBeatTimeout = 35 seconds
  var heartBeatTask: Option[Cancellable] = None

  setTimer()

  def session = context.parent

  /**
   * returns Boolean determines if the transport is still ready for sending more messages
   */
  def sendFrame(m: String): Boolean

  def receive: Receive = {
    case SockJsSession.OpenMessage =>
      if (sendFrame(SockJsFrames.OPEN_FRAME)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.Message(m) =>
      setTimer()
      if (sendFrame(m)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.HeartBeat =>
      setTimer()
      if (sendFrame(SockJsFrames.HEARTBEAT_FRAME)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.Close(code, reason) =>
      sendFrame(SockJsFrames.closingFrame(code, reason))
      self ! PoisonPill
  }

  def setTimer() {
    for (h <- heartBeatTask) h.cancel()
    heartBeatTask = Some(context.system.scheduler.scheduleOnce(heartBeatTimeout, self, PoisonPill))
  }
}

class TransportController extends Controller {
  import akka.pattern.ask
  import concurrent.ExecutionContext.Implicits.global
  implicit val Timeout = akka.util.Timeout(1000)

  def randomNumber() = 2L << 30 + Random.nextInt

  def cors(implicit req: Request[AnyContent]) = Seq(
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    ACCESS_CONTROL_ALLOW_ORIGIN -> req.headers.get("origin").map(o => if (o != "null") o else "*").getOrElse("*"))
    .union(
      (for (acrh ← req.headers.get(ACCESS_CONTROL_REQUEST_HEADERS))
      yield ACCESS_CONTROL_ALLOW_HEADERS -> acrh).toSeq)

  def withExistingSession(service: String, session: String)(f: ActorRef => Future[SimpleResult]): Future[SimpleResult] =
    SockJsSystem.service(service) match {
      case Some(s) =>
        s ? SockJsService.GetSession(session) flatMap {
          case ss: ActorRef =>
            f(ss)
          case SockJsService.SessionNotFound =>
            Future.successful(NotFound)
        }
      case None => Future successful NotFound
    }

  def withSession(service: String, session: String)(f: ActorRef => SimpleResult): Future[SimpleResult] =
    SockJsSystem.service(service) match {
      case Some(s) =>
        s ? SockJsService.GetOrCreateSession(session) map {
          case ss: ActorRef =>
            f(ss)
          case SockJsService.SessionNotFound =>
            NotFound
        }
      case None => Future successful NotFound
    }
}

object Transport {
  val WEBSOCKET = "websocket"
  val EVENT_SOURCE = "eventsource"
  val HTML_FILE = "htmlfile"
  val JSON_P = "jsonp"
  val XHR = "xhr"
  val XHR_SEND = "xhr_send"
  val XHR_STREAMING = "xhr_streaming"
  val JSON_P_SEND = "jsonp_send"

  val CONTENT_TYPE_JAVASCRIPT = "application/javascript; charset=UTF-8"
  val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
  val CONTENT_TYPE_PLAIN = "text/plain; charset=UTF-8"
  val CONTENT_TYPE_HTML = "text/html; charset=UTF-8"
}