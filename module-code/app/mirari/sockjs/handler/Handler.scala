package mirari.sockjs.handler

import akka.actor.{ActorRef, Actor}
import play.api.libs.json.JsValue

/**
 * @author alari
 * @since 12/10/13
 */
abstract class Handler(session: ActorRef) extends Actor{
  def receive = {
    case m: JsValue =>
      onMessage(m)
  }

  def onMessage(msg: JsValue)
}

object Handler {

}