package mirari.sockjs.handler

import akka.actor.ActorRef
import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/10/13
 */
class EchoHandler(s: ActorRef) extends Handler(s){
  def onMessage(msg: JsValue) = ???
}
