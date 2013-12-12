package mirari.sockjs.handler

import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/10/13
 */
class EchoHandler extends SockJsHandler {
  def onMessage(msg: JsValue) = {
    send(msg)
  }
}
