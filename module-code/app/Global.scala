import akka.actor.Props
import mirari.sockjs.handler.{CloseHandler, EchoHandler}
import mirari.sockjs.SockJsSystem
import play.api.GlobalSettings

/**
 * @author alari
 * @since 12/9/13
 */
object Global extends GlobalSettings {
  override def onStart(app: play.api.Application) {
    play.api.Logger.debug("Global.onStart()")

    SockJsSystem.initService("echo", Props[EchoHandler])
    SockJsSystem.initService("disabled_websocket_echo", Props[EchoHandler], websocket = false)
    SockJsSystem.initService("cookie_needed_echo", Props[EchoHandler], cookieNeeded = true)
    SockJsSystem.initService("close", Props[CloseHandler])
  }
}
