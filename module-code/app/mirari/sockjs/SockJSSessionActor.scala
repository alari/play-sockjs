package mirari.sockjs

import play.api.libs.json.{JsArray, JsValue}
import akka.actor.Actor
import play.api.libs.iteratee.{Iteratee, Concurrent}
import mirari.sockjs.SockJSService.{CloseFrame, OpenFrame}
import scala.concurrent.ExecutionContext

/**
 * @author alari
 * @since 12/9/13
 */
object SockJSSessionActor {

  case class IncomingMessage(msg: JsValue)
  case class IncomingMessages(msgs: JsArray)

  case object ConnectionInterrupted
  case object OpenConnection

}

abstract class SockJSSessionActor(val session: String) extends Actor {
  import SockJSSessionActor._
  import ExecutionContext.Implicits.global

  var channel: Concurrent.Channel[JsValue] = null

  // Output enumerator
  val out = Concurrent.unicast[JsValue](onStart = {c =>
    channel = c
  })

  // Input iteratee
  val in = Iteratee.foreach[JsValue]{
    msg =>
      self ! IncomingMessage(msg)
  } map {
    _ =>
    // Kill this actor when connection is broken
      self ! ConnectionInterrupted
  }

  var connections = 0

  def receive = {
    case IncomingMessage(msg) =>
      play.api.Logger.debug(msg.toString())

    case ConnectionInterrupted =>
      play.api.Logger.debug("connection interrupted")

    case OpenConnection =>
      if(connections == 0) {
        connections += 1
        sender ! OpenFrame
      } else {
        sender ! CloseFrame
      }
  }
}
