package mirari.sockjs.impl

import akka.actor.ActorSystem

/**
 * @author alari
 * @since 12/13/13
 */
object SockJs {
  implicit val system = ActorSystem("sockjs")
}
