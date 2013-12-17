package mirari.sockjs.channels

import akka.actor._
import mirari.sockjs.SockJs
import akka.actor.Terminated
import scala.Some
import scala.concurrent.duration.FiniteDuration

/**
 * @author alari
 * @since 12/17/13
 */
class ChannelLeaf extends Actor {
  def receive = branchBehaviour orElse pubSubBehaviour

  private var listeners = Set[ActorRef]()
  private var timeout: Option[Cancellable] = None

  import context.dispatcher
  val timeoutDelay = FiniteDuration(10, "seconds")

  def checkTimeout() {
    if (listeners.isEmpty) launchTimeout()
  }

  def clearTimeout() {
    timeout.map(_.cancel())
    timeout = None
  }

  def launchTimeout() {
    if(timeout.isEmpty) timeout = Some(context.system.scheduler.scheduleOnce(timeoutDelay, self, PoisonPill))
  }

  launchTimeout()

  def subscribe(actor: ActorRef) {
    listeners += actor
    context.watch(actor)
    clearTimeout()
  }

  def unsubscribe(actor: ActorRef) {
    listeners -= actor
    context.unwatch(actor)
    checkTimeout()
  }

  def broadcast(message: Any) {
    listeners.foreach(_ ! message)
  }

  def canSubscribe(state: Any) = {
    true
  }

  def handle(state: Any, message: Any) {
    if (canSubscribe(state)) broadcast(message)
  }

  val pubSubBehaviour: Receive = {
    case Terminated(a) if listeners.contains(a) =>
      unsubscribe(a)

    case ChannelLeaf.Broadcast(m) =>
      broadcast(m)

    case ChannelLeaf.Subscribe(state) if canSubscribe(state) =>
      subscribe(sender)

    case ChannelLeaf.Unsubscribe if listeners.contains(sender) =>
      unsubscribe(sender)

    case ChannelLeaf.Handle(state, msg) =>
      handle(state, msg)
  }

  val branchBehaviour: Receive = {
    import ChannelsBranch._
    import akka.pattern.{ask, pipe}
    import SockJs.Timeout

    def getChild(name: String, props: Props) =
      context.child(name) match {
        case Some(a) => a
        case None => context.actorOf(props, name)
      }

    {
      case GetChannel(p, n :: Nil) =>
        sender ! getChild(n, Props[ChannelsBranch])

      case GetChannel(p, n :: tail) =>
        val s = sender
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
  }
}

object ChannelLeaf {

  case class Broadcast(message: Any)

  case class Subscribe(listenerState: Any)

  case class Handle(senderState: Any, message: Any)

  case object Unsubscribe

}