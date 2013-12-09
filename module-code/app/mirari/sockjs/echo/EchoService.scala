package mirari.sockjs.echo

import mirari.sockjs.SockJSService.Info
import akka.actor.Props
import mirari.sockjs.SockJSService

/**
 * @author alari
 * @since 12/9/13
 */
class EchoService extends SockJSService {
  def info = Info(
    websocket = true,
    cookie_needed = false
  )

  def sessionProps(session: String) = Props(classOf[EchoActor], session)
}