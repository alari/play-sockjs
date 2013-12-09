import akka.actor.Props
import mirari.sockjs.echo.{DisabledWebsocketEchoService, EchoService}
import mirari.sockjs.SockJS
import play.api.GlobalSettings

/**
 * @author alari
 * @since 12/9/13
 */
object Global extends GlobalSettings {
  override def onStart(app : play.api.Application) {
    SockJS.registerService("echo", Props[EchoService])
    SockJS.registerService("disabled_websocket_echo", Props[DisabledWebsocketEchoService])
  }
}
