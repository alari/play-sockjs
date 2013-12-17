package mirari.sockjs.impl

import mirari.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
object EchoService extends SockJsService {
  override val maxBytesSent = 4096
  override val maxBytesReceived = 5000
}
