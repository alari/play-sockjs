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
class Session(handlerProps: Props) extends Actor {

  import Session._

  def receive = connectingState orElse queueingState orElse timeoutingState

  val handler = context.actorOf(handlerProps, "handler")

  val SessionTimeout = FiniteDuration(10, "seconds")
  val HeartbeatPeriod = FiniteDuration(35, "seconds")

  var transportId = 0
  var queue = Queue[JsValue]()
  var transport: Option[ActorRef] = None
  var timeout: Option[Cancellable] = Some(context.system.scheduler.scheduleOnce(SessionTimeout, self, TriggerTimeout))
  var heartbeat: Option[Cancellable] = None
  var openWritten = false



  def connectingState: Receive = {
    case CreateTransport(props, request) =>
      transportId += 1
      val a = context.actorOf(props, s"transport.$transportId")
      a ! RegisterTransport
      sender ! a
      handler ! request

    case RegisterTransport if !openWritten =>
      sender ! OutgoingRaw(Frames.Open)

    case RegisterTransport =>
      registerTransport(sender)
  }

  def listeningState: Receive = {
    case Incoming(msg) =>
      try {
        JsonCodec.decodeJson(msg) match {
          case JsArray(msgs) =>
            msgs foreach (m => handler ! Handler.Incoming(m))
          case m: JsValue =>
            handler ! m
        }
      } catch {
        case e: Throwable =>
          play.api.Logger.error("Cannot parse json", e)
          self ! Close
      }
  }

  def queueingState: Receive = {
    case OutgoingMessage(msg) =>
      enqueueMessage(msg)

    case OutgoingRaw(msg) =>
      sendFrame(msg)

    case Heartbeat =>
      sendFrame(Frames.Heartbeat)
  }

  def timeoutingState: Receive = {
    case TriggerTimeout =>
      self ! PoisonPill
  }







  def sendFrame(frame: String) {
    transport.map {
      t =>
        t ! OutgoingRaw(frame)
        resetTransport()
    }
  }

  def enqueueMessage(message: JsValue) {
    transport match {
      case Some(t) =>
        t ! OutgoingRaw(s"a[$message]")
        resetTransport()
      case None =>
        queue = queue.enqueue(message)
    }
  }

  def resetTransport() {
    transport = None
    timeout.map(_.cancel())
    heartbeat.map(_.cancel())
    timeout = Some(context.system.scheduler.scheduleOnce(SessionTimeout, self, TriggerTimeout))
  }

  def registerTransport(t: ActorRef) {
    timeout.map(_.cancel())
    transport match {
      case Some(tr) if tr == t =>
      // do nothing
      case Some(tr) =>
        tr ! OutgoingRaw(Frames.closed(2010, "Another connection still open"))
      case None =>
        transport = Some(t)
        heartbeat = Some(context.system.scheduler.schedule(HeartbeatPeriod, HeartbeatPeriod, self, Heartbeat))
        processQueue()
    }
  }

  def processQueue(): Unit = if (!queue.isEmpty) {
    transport.map {
      t =>
        t ! OutgoingRaw("a" + JsArray(queue.toSeq))
        queue = Queue()
        resetTransport()
    }
  }




  override def postStop() {
    heartbeat.map(_.cancel())
    timeout.map(_.cancel())
  }
}

object Session {

  case class Incoming(msg: String)

  case class OutgoingRaw(msg: String)

  case class OutgoingMessage(msg: JsValue)

  case class CreateTransport(props: Props, request: RequestHeader)

  case object RegisterTransport

  case object TriggerTimeout

  case object Heartbeat

  case object Close

}