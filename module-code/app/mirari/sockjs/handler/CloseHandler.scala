package mirari.sockjs.handler

import play.api.libs.json.JsValue
import mirari.sockjs.service.SockJsSession

/**
 * @author alari
 * @since 12/10/13
 */
class CloseHandler extends Handler {
  context.parent ! SockJsSession.Close

  def onMessage(msg: JsValue) = send(msg)
}
