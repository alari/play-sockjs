package mirari.sockjs.echo

import mirari.sockjs.SockJSService
import mirari.sockjs.SockJSService.Info
import akka.actor.Props

/**
 * @author alari
 * @since 12/9/13
 */
class DisabledWebsocketEchoService extends SockJSService {
  def info = Info(
    websocket = false,
    cookie_needed = false
  )

  def sessionProps(session: String) = Props(classOf[EchoActor], session)
}