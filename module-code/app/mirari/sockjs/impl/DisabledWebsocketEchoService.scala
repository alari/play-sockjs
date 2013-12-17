package mirari.sockjs.impl

import akka.actor.Props
import mirari.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
object DisabledWebsocketEchoService extends SockJsService{
  override val websocketEnabled = false
}
