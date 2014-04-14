package infra.sockjs.impl

import infra.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
object DisabledWebsocketEchoService extends SockJsService{
  override val websocketEnabled = false
}
