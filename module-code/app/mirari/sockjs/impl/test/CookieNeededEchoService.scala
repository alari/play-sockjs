package mirari.sockjs.impl.test

import mirari.sockjs.impl.{SockJsHandler, SockJsService}
import akka.actor.Props

/**
 * @author alari
 * @since 12/16/13
 */
object CookieNeededEchoService extends SockJsService{
  override val cookieNeeded = true
}
