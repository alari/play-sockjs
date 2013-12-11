package mirari.sockjs.handler

import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/10/13
 */
class EchoHandler extends Handler {
  def onMessage(msg: JsValue) = {
    play.api.Logger.debug("ECHO  "+msg)
    send(msg)
  }
}
