package mirari.sockjs.transport

import akka.actor.{PoisonPill, Cancellable, ActorRef, Actor}
import akka.event.Logging
import mirari.sockjs.service.SockJsSession
import mirari.sockjs.frames.SockJsFrames
import scala.concurrent.duration.DurationInt

/**
 * @author alari
 * @since 12/10/13
 */
abstract class TransportActor(session: ActorRef) extends Actor {
  import scala.language.postfixOps
  implicit val executionContext = context.system.dispatcher

  val logger = Logging(context.system, this)
  val heartBeatTimeout = 35 seconds
  var heartBeatTask: Option[Cancellable] = None

  setTimer()

  /**
   * returns Boolean determines if the transport is still ready for sending more messages
   */
  def sendFrame(m: String): Boolean

  def receive: Receive = {
    case SockJsSession.OpenMessage =>
      if (sendFrame(SockJsFrames.OPEN_FRAME)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.Message(m) =>
      setTimer()
      if (sendFrame(m)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.HeartBeat =>
      setTimer()
      if (sendFrame(SockJsFrames.HEARTBEAT_FRAME)) session ! SockJsSession.Register
      else self ! PoisonPill

    case SockJsSession.Close(code, reason) =>
      sendFrame(SockJsFrames.closingFrame(code, reason))
      self ! PoisonPill
  }

  def setTimer() {
    for (h <- heartBeatTask) h.cancel()
    heartBeatTask = Some(context.system.scheduler.scheduleOnce(heartBeatTimeout, self, PoisonPill))
  }
}