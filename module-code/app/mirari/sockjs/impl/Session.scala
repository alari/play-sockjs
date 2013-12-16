package mirari.sockjs.impl

import akka.actor._
import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.Queue
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.libs.json.JsArray
import scala.Some

/**
 * @author alari
 * @since 12/13/13
 */
class Session(handlerProps: Props, timeoutMs: Int = SockJs.SessionTimeoutMs, heartbeatPeriodMs: Int = SockJs.SessionHeartbeatMs) extends Actor {

  import Session._
  import context.dispatcher

  def receive = connectingBehaviour

  val handler = context.actorOf(handlerProps, "handler")

  val SessionTimeout = FiniteDuration(timeoutMs, "milliseconds")
  val HeartbeatPeriod = FiniteDuration(heartbeatPeriodMs, "milliseconds")

  var transportId = 0
  var pendingMessagesQueue = Queue[JsValue]()
  var transport: Option[ActorRef] = None
  var timeout: Option[Cancellable] = None
  var heartbeat: Option[Cancellable] = None
  var openWritten = false

  launchTimeout()

  def becomeOpen() = {
    timeout.map(_.cancel())
    launchHeartbeat()
    context.become(openBehaviour, discardOld = true)
  }

  def becomeClosed() = {
    launchTimeout()
    heartbeat.map(_.cancel())
    context.become(closedBehaviour, discardOld = true)
  }

  def becomeConnecting() = {
    launchTimeout()
    heartbeat.map(_.cancel())
    context.become(connectingBehaviour, discardOld = true)
  }

  val sendToTransport: Receive = {
    def sendFrame(frame: String) {
      transport.map(_ ! OutgoingRaw(frame))
      transport = None
      becomeConnecting()
    }
    {
      case Heartbeat =>
        sendFrame(Frames.Heartbeat)

      case OutgoingMessage(msg) =>
        sendFrame(Frames.array(msg))

      case OutgoingRaw(msg) =>
        sendFrame(msg)
    }
  }


  val unregisterTransport: Receive = {
    case UnregisterTransport =>
      transport.map {
        t =>
          t ! OutgoingRaw(Frames.Closed_ConnectionInterrupted)
          t ! PoisonPill
      }
      transport = None
      becomeConnecting()
  }

  val closeSession: Receive = {
    case Close =>
      transport.map {
        t =>
          t ! OutgoingRaw(Frames.Closed_GoAway)
          t ! PoisonPill
      }
      transport = None
      becomeClosed()
  }

  val waitForTransport: Receive = {
    // Create a new transport
    case CreateTransport(props, request) =>
      transportId += 1
      val a = context.actorOf(props, s"transport.$transportId")
      a ! RegisterTransport
      sender ! a
      handler ! request

    // Register a new transport
    case RegisterTransport =>
      if (!openWritten) {
        // First frame for this session
        openWritten = true
        sender ! OutgoingRaw(Frames.Open)
        becomeConnecting()
      } else if (pendingMessagesQueue.isEmpty) {
        // Register a transport, wait for messages
        transport = Some(sender)
        launchHeartbeat()
        becomeOpen()
      } else {
        // Send pendings -- do not register
        sender ! OutgoingRaw(Frames.array(pendingMessagesQueue.toSeq))
        pendingMessagesQueue = Queue()
        becomeConnecting()
      }
  }

  val enqueueMessages: Receive = {
    case OutgoingMessage(msg) =>
      pendingMessagesQueue = pendingMessagesQueue.enqueue(msg)
  }

  val waitForTimeout: Receive = {
    case TriggerTimeout =>
      self ! PoisonPill
  }

  val handleIncomings: Receive = {
    case Incoming(msg) =>
      try {
        JsonCodec.decodeJson(msg) match {
          case JsArray(msgs) =>
            msgs foreach (m => handler ! SockJsHandler.Incoming(m))
          case m: JsValue =>
            handler ! SockJsHandler.Incoming(m)
        }
      } catch {
        case e: Throwable =>
          play.api.Logger.debug("Cannot parse json", e)
          self ! Close
      }
    case IncomingJson(JsArray(msgs)) =>
      msgs foreach (m => handler ! SockJsHandler.Incoming(m))
    case IncomingJson(m) =>
      handler ! SockJsHandler.Incoming(m)
  }

  def rejectConnections(reason: String): Receive = {
    case CreateTransport(props, request) =>
      transportId += 1
      val a = context.actorOf(props, s"transport.$transportId")
      a ! OutgoingRaw(reason)
      a ! PoisonPill
      sender ! a

    case RegisterTransport =>
      sender ! OutgoingRaw(reason)
      sender ! PoisonPill
  }


  val openBehaviour =
    unregisterTransport orElse
      rejectConnections(Frames.Closed_AnotherConnectionStillOpen) orElse
      handleIncomings orElse
      sendToTransport orElse
      closeSession

  val closedBehaviour =
    rejectConnections(Frames.Closed_GoAway) orElse
      waitForTimeout

  val connectingBehaviour =
    waitForTransport orElse
      enqueueMessages orElse
      handleIncomings orElse
      waitForTimeout orElse
      closeSession


  def launchHeartbeat() = {
    heartbeat.map(_.cancel())
    heartbeat = Some(context.system.scheduler.schedule(HeartbeatPeriod, HeartbeatPeriod, self, Heartbeat))
  }

  def launchTimeout() = {
    timeout.map(_.cancel())
    timeout = Some(context.system.scheduler.scheduleOnce(SessionTimeout, self, TriggerTimeout))
  }

  override def postStop() {
    heartbeat.map(_.cancel())
    timeout.map(_.cancel())
  }
}

object Session {

  case class Incoming(msg: String)

  case class IncomingJson(msg: JsValue)

  case class OutgoingRaw(msg: String)

  case class OutgoingMessage(msg: JsValue)

  case class CreateTransport(props: Props, request: RequestHeader)

  case object RegisterTransport

  case object UnregisterTransport

  case object TriggerTimeout

  case object Heartbeat

  case object Close

}