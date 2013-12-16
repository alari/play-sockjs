package mirari.sockjs.impl

import play.api.mvc.{Action, Results, Handler}
import akka.actor.ActorRef

import mirari.sockjs.impl.api._

/**
 * @author alari
 * @since 12/16/13
 */
abstract sealed class SockJsAction {
  def handler(service: ActorRef): Handler = StaticController.greeting
}

case object Greetings extends SockJsAction {
  override def handler(service: ActorRef) = StaticController.greeting
}

case object Iframe extends SockJsAction {
  override def handler(service: ActorRef) = StaticController.iframe
}

case object InfoOptions extends SockJsAction {
  override def handler(service: ActorRef): Handler = StaticController.infoOpts
}

case object Info extends SockJsAction

case object RawWebsockset extends SockJsAction {
  override def handler(service: ActorRef) = StaticController.websocket
}

case class Jsonp(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = JsonpTransportController.jsonp(service, session)
}

case class JsonpSend(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = JsonpTransportController.jsonpSend(service, session)
}

case class XhrPolling(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = XhrTransportController.xhrPolling(service, session)
}

case class XhrPollingOptions(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = XhrTransportController.opts
}

case class XhrSend(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = XhrTransportController.xhrSend(service, session)
}

case class XhrSendOptions(session: String) extends SockJsAction{
  override def handler(service: ActorRef) = XhrTransportController.opts
}

case class XhrStreaming(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = XhrTransportController.xhrStream(service, session)
}

case class XhrStreamingOptions(session: String) extends SockJsAction{
  override def handler(service: ActorRef) = XhrTransportController.opts
}

case class EventSource(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = EventsourceTransportController.eventsource(service, session)
}

case class HtmlFile(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = HtmlFileTransportController.htmlfile(service, session)
}

case class WebSocket(session: String) extends SockJsAction {
  override def handler(service: ActorRef) = WebsocketTransportController.websocket(service, session)
}

case object NotFound extends SockJsAction {
  override def handler(service: ActorRef) = Action {
    Results.NotFound
  }
}