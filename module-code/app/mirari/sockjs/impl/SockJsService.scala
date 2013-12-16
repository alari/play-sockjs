package mirari.sockjs.impl

import play.core.Router
import play.api.mvc.{RequestHeader, Handler}
import scala.runtime.AbstractPartialFunction
import akka.actor.{ActorRef, Props}
import mirari.sockjs.impl.api.StaticController

/**
 * @author alari
 * @since 12/16/13
 */
trait SockJsService extends Router.Routes {

  import SockJsService._

  private var path: String = ""
  private var serviceName: String = ""
  private var service: ActorRef = SockJs.system.deadLetters

  def setPrefix(prefix: String) {
    serviceName = prefix.substring(prefix.lastIndexOf('/') + 1)

    import SockJs.system.dispatcher

    SockJs.registerService(serviceName, Service.Params(
      handlerProps,
      sessionTimeoutMs,
      heartbeatPeriodMs)).map(r => service = r)

    path = prefix
  }

  def prefix = path

  def documentation = Nil

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {

    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) = {
      if (rh.path.startsWith(path)) {
        if (rh.path == path + "/info" && rh.method == GET) {
          StaticController.info(websocketEnabled, cookieNeeded)
        } else dispatch(rh.method, rh.path.drop(path.length)).handler(service)
      } else {
        default(rh)
      }
    }

    def isDefinedAt(rh: RequestHeader) = rh.path.startsWith(path)
  }

  val handlerProps: Props = Props[SockJsHandler.Echo]
  val websocketEnabled: Boolean = true
  val cookieNeeded: Boolean = false
  val sessionTimeoutMs: Int = SockJs.SessionTimeoutMs
  val heartbeatPeriodMs: Int = SockJs.SessionHeartbeatMs
}

object SockJsService {

  def serverSesR(suffix: String) = ("^/[^/.]+/([^/.]+)" + suffix + "$").r

  val GreetingsR = "^/?$".r
  val IframeR = "^/iframe[0-9-.a-z_]*.html$".r
  val InfoR = "^/info$".r
  val RawWebsocketR = "^/websocket$".r

  val JsonpR = serverSesR("/jsonp")
  val JsonpSendR = serverSesR("/jsonp_send")
  val XhrR = serverSesR("/xhr")
  val XhrSendR = serverSesR("/xhr_send")
  val XhrStreamingR = serverSesR("/xhr_streaming")
  val EventSourceR = serverSesR("/eventsource")
  val HtmlFileR = serverSesR("/htmlfile")
  val WebsocketR = serverSesR("/websocket")

  val GET = "GET"
  val POST = "POST"
  val OPTIONS = "OPTIONS"

  def dispatch(method: String, resource: String): SockJsAction = (method, resource) match {
    case (GET, GreetingsR()) => Greetings
    case (GET, IframeR()) => Iframe
    case (GET, InfoR()) => Info
    case (OPTIONS, InfoR()) => InfoOptions
    case (GET, RawWebsocketR()) => RawWebsockset

    case (GET, JsonpR(session)) => Jsonp(session)
    case (POST, JsonpSendR(session)) => JsonpSend(session)

    case (POST, XhrR(session)) => XhrPolling(session)
    case (OPTIONS, XhrR(session)) => XhrPollingOptions(session)

    case (POST, XhrSendR(session)) => XhrSend(session)
    case (OPTIONS, XhrSendR(session)) => XhrSendOptions(session)

    case (POST, XhrStreamingR(session)) => XhrStreaming(session)
    case (OPTIONS, XhrStreamingR(session)) => XhrStreamingOptions(session)

    case (GET, EventSourceR(session)) => EventSource(session)

    case (GET, HtmlFileR(session)) => HtmlFile(session)

    case (GET, WebsocketR(session)) => WebSocket(session)

    case _ => NotFound
  }
}