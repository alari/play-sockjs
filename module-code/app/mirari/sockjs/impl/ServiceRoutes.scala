package mirari.sockjs.impl

import play.core.Router
import play.api.mvc.{RequestHeader, Handler}
import scala.runtime.AbstractPartialFunction

/**
 * @author alari
 * @since 12/16/13
 */
case class ServiceRoutes(service: String) extends Router.Routes {

  import ServiceRoutes._

  private var path: String = ""

  def setPrefix(prefix: String) {
    path = prefix
  }

  def prefix = path

  def documentation = Nil

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {

    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) = {

      if (rh.path.startsWith(path)) {
        dispatch(rh.method, rh.path.drop(path.length)).handler(service)
      } else {
        default(rh)
      }
    }

    def isDefinedAt(rh: RequestHeader) = rh.path.startsWith(path)
  }


}

object ServiceRoutes {
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

  abstract sealed class SockJsAction {
    def handler(service: String): Handler = ???
  }

  case object Greetings extends SockJsAction

  case object Iframe extends SockJsAction

  case object InfoOptions extends SockJsAction

  case object Info extends SockJsAction

  case object RawWebsockset extends SockJsAction

  case class Jsonp(session: String) extends SockJsAction

  case class JsonpSend(session: String) extends SockJsAction

  case class XhrPolling(session: String) extends SockJsAction

  case class XhrPollingOptions(session: String) extends SockJsAction

  case class XhrSend(session: String) extends SockJsAction

  case class XhrSendOptions(session: String) extends SockJsAction

  case class XhrStreaming(session: String) extends SockJsAction

  case class XhrStreamingOptions(session: String) extends SockJsAction

  case class EventSource(session: String) extends SockJsAction

  case class HtmlFile(session: String) extends SockJsAction

  case class WebSocket(session: String) extends SockJsAction

  case object NotFound extends SockJsAction

}