package mirari.sockjs.impl

import play.api.mvc.{EssentialAction, Action, Results, Handler}
import akka.actor.ActorRef
import scala.concurrent.Future

import Results._
import mirari.sockjs.impl.api.StaticController

/**
 * @author alari
 * @since 12/16/13
 */
abstract sealed class SockJsAction{
  import SockJs.Timeout
  import SockJs.system.dispatcher
  import akka.pattern.ask

  def handler(service: ActorRef): Handler = t.test

}

object t extends play.api.mvc.Controller {
  def test = TODO
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

case object NotFound extends SockJsAction {
  override def handler(service: ActorRef) = Action{Results.NotFound}
}