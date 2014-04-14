package infra.sockjs.impl

import infra.sockjs.SockJsService

/**
 * @author alari
 * @since 12/16/13
 */
object EchoService extends SockJsService {
  override val maxBytesSent = 4096
  override val maxBytesReceived = 5000
}
