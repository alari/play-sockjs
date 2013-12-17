package mirari.sockjs.channels

import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.{ask, pipe}
import mirari.sockjs.SockJs

/**
 * @author alari
 * @since 12/17/13
 */
class ChannelsBranch extends Actor {

  import ChannelsBranch._

  import SockJs.Timeout

  def receive = {
    case GetChannel(p, n :: Nil) =>
      sender ! getChild(n, Props[ChannelsBranch])

    case GetChannel(p, n :: tail) =>
      val s = sender
      import context.dispatcher
      getChild(n, Props[ChannelsBranch]) ? GetChannel(p, tail) pipeTo s

    case PushToChannel(msg, n :: Nil) =>
      context.child(n).map(_ ! msg)

    case PushToChannel(msg, n :: tail) =>
      context.child(n).map(_ ! PushToChannel(msg, tail))

    case TellToChannel(p, msg, n :: Nil) =>
      getChild(n, p).tell(msg, sender)

    case TellToChannel(p, msg, n :: tail) =>
      getChild(n, p).tell(TellToChannel(p, msg, tail), sender)
  }

  def getChild(name: String, props: Props) =
    context.child(name) match {
      case Some(a) => a
      case None => context.actorOf(props, name)
    }
}

object ChannelsBranch {

  case class GetChannel(props: Props, path: List[String])

  case class PushToChannel(msg: Any, path: List[String])

  case class TellToChannel(props: Props, msg: Any, path: List[String])

}