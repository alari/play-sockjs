package infra.sockjs.impl

import akka.actor.Props
import infra.sockjs.{SockJsHandler, SockJsService}

/**
 * @author alari
 * @since 12/16/13
 */
object CloseService extends SockJsService{
  override val handlerProps = Props[SockJsHandler.Closed]
}
