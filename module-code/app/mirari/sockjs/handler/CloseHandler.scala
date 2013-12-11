package mirari.sockjs.handler

import play.api.libs.json.JsValue
import mirari.sockjs.service.SockJsSession
import mirari.sockjs.frames.SockJsFrames

/**
 * @author alari
 * @since 12/10/13
 */
class CloseHandler extends SockJsHandler {
  context.parent ! SockJsSession.Close(2010, "Closed")

  def onMessage(msg: JsValue) = send(msg)
}
