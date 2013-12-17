package mirari.sockjs

import play.api.mvc.{Action, Results, Handler}

/**
 * @author alari
 * @since 12/16/13
 */
abstract sealed class SockJsAction {
  def handler(service: SockJsService): Handler = Action {
    Results.NotFound
  }
}

object SockJsAction {

  case object Greetings extends SockJsAction {
    override def handler(service: SockJsService) = service.greeting
  }

  case object Iframe extends SockJsAction {
    override def handler(service: SockJsService) = service.iframe
  }

  case object InfoOptions extends SockJsAction {
    override def handler(service: SockJsService): Handler = service.infoOpts
  }

  case object Info extends SockJsAction {
    override def handler(service: SockJsService) = service.info
  }

  case object RawWebsockset extends SockJsAction {
    override def handler(service: SockJsService) = service.rawWebsocket
  }

  case class Jsonp(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.jsonp(session)
  }

  case class JsonpSend(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.jsonpSend(session)
  }

  case class XhrPolling(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrPolling(session)
  }

  case class XhrPollingOptions(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrOptions
  }

  case class XhrSend(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrSend(session)
  }

  case class XhrSendOptions(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrOptions
  }

  case class XhrStreaming(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrStream(session)
  }

  case class XhrStreamingOptions(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.xhrOptions
  }

  case class EventSource(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.eventsource(session)
  }

  case class HtmlFile(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = service.htmlfile(session)
  }

  case class WebSocket(session: String) extends SockJsAction {
    override def handler(service: SockJsService) = if (service.websocketEnabled) service.websocket(session)
    else super.handler(service)
  }

  case object NotFound extends SockJsAction

}

