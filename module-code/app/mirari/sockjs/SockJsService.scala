package mirari.sockjs

import play.core.Router
import mirari.sockjs.transport.SockJsController
import akka.actor.{Props, ActorRef}
import scala.runtime.AbstractPartialFunction
import play.api.mvc.{Handler, RequestHeader}

/**
 * @author alari
 * @since 12/16/13
 */
trait SockJsService extends Router.Routes with SockJsController {

  self =>


  private var path: String = ""
  private var serviceName: String = ""
  private var serviceActor: ActorRef = SockJs.system.deadLetters

  def service = serviceActor

  def setPrefix(prefix: String) {
    serviceName = prefix.substring(prefix.lastIndexOf('/') + 1)

    import scala.concurrent.ExecutionContext.Implicits.global

    SockJs.registerService(serviceName, Service.Params(
      handlerProps,
      sessionTimeoutMs,
      heartbeatPeriodMs)).map(r => serviceActor = r)

    path = prefix
  }

  def prefix = path

  def documentation = Nil

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {

    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) = {
      if (rh.path.startsWith(path)) {
        SockJsService.dispatch(rh.method, rh.path.drop(path.length)).handler(self)
      } else {
        default(rh)
      }
    }

    def isDefinedAt(rh: RequestHeader) = rh.path.startsWith(path)
  }

  val handlerProps: Props = Props[SockJsHandler.Echo]
  val websocketEnabled: Boolean = true
  val cookieNeeded: Boolean = false
  val sessionTimeoutMs: Int = 10000
  val heartbeatPeriodMs: Int = 25000

  val maxBytesSent = 128*1024
  val maxBytesReceived = 64*1024
  val clientScriptSrc = "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.min.js"
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

  import SockJsAction._

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