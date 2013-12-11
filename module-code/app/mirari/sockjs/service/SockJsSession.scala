package mirari.sockjs.service

import akka.actor._
import akka.event.Logging
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.JsArray
import scala.Some
import scala.concurrent.duration.DurationInt
import mirari.sockjs.frames.JsonCodec
import play.api.mvc.RequestHeader

/**
 * @author alari
 * @since 12/10/13
 */
class SockJsSession(handlerProps: Props) extends Actor {

  val handler = context.actorOf(handlerProps, "handler")

  val heartBeatPeriod = 25000

  //val logger = Logging(context.system, this)
  val logger = play.api.Logger
  val pendingWrites = scala.collection.mutable.Queue[JsValue]()
  var transportListener: Option[ActorRef] = None
  var heartBeatTask: Option[Cancellable] = None
  var timer: Option[Cancellable] = None
  var openWriten = false
  var closeMessage = SockJsSession.Close(3000, "Go away!")
  //TODO: Max Queue Size

  implicit val executionContext = context.system.dispatcher

  var transportId = 0

  val (downEnumerator, downChannel) = Concurrent.broadcast[JsValue]
  val upIteratee = Iteratee.foreach[JsValue] {
    msg =>
      //downChannel push msg
      println(s"handler1 ::::::::::: message: $msg")
      handler ! msg
  }

  val (upEnumerator, upChannel) = Concurrent.broadcast[JsValue]
  val downIteratee = Iteratee.foreach[JsValue](msg ⇒ self ! SockJsSession.Outgoing(msg))

  upEnumerator |>> upIteratee
  downEnumerator |>> downIteratee

  setTimer()

  //if no transportListener is attached for 6 seconds, then consider the client is dead and close the session

  def receive = connecting orElse timeout

  def connecting: Receive = {
    case SockJsSession.Register ⇒
      logger.debug(s"state: CONNECTING, sender: $sender, message: ${SockJsSession.Register}")
      (register andThen sendOpenMessage andThen resetListener andThen becomeOpen)(sender)

    case c@SockJsSession.CreateAndRegister(props, name, request) =>
      logger.debug(s"state: CONNECTING, sender: $sender, message: $c")
      createAndRegister(props, name, request)


    case c: SockJsSession.Close ⇒
      logger.debug(s"state: CONNECTING, sender: $sender, message: $c")
      becomeClosed.apply()
  }

  def timeout: Receive = {
    case SockJsSession.Timeout ⇒ doClose()
  }

  def open: Receive = {
    case c@SockJsSession.CreateAndRegister(props, name, request) =>
      logger.debug(s"state: OPEN, sender: $sender, message: $c")
      createAndRegister(props, name, request)

    // Registering a transport
    case SockJsSession.Register ⇒
      logger.debug(s"state: OPEN, sender: $sender, message: ${SockJsSession.Register}")
      register(sender)
      if (!pendingWrites.isEmpty) (writePendingMessages andThen resetListener)(sender)

      // A message received
    case s@SockJsSession.Incoming(msgs) ⇒
      logger.debug(s"state: OPEN, sender: $sender, message: $s")
      handleMessages(msgs)

      // Writing a message to output
    case w@SockJsSession.Outgoing(msg) ⇒
      logger.debug(s"state: OPEN, sender: $sender, message: $w, transport: $transportListener")
      enqueue(msg)
      transportListener map (writePendingMessages andThen resetListener)

      // Sending a heartbeat
    case SockJsSession.HeartBeat ⇒
      logger.debug(s"state: OPEN, sender: $sender, message: ${SockJsSession.HeartBeat}")
      transportListener map (sendHeartBeatMessage andThen resetListener)

      // Closing the session
    case c: SockJsSession.Close ⇒
      logger.debug(s"state: OPEN, sender: $sender, message: $c")
      this.closeMessage = c
      transportListener map (sendCloseMessage andThen resetListener andThen becomeClosed) getOrElse becomeClosed
  }

  def closed: Receive = {
    case c@SockJsSession.CreateAndRegister(props, name, request) =>
      logger.debug(s"state: CLOSED, sender: $sender, message: $c")
      createAndRegister(props, name, request)

    case SockJsSession.Register if !openWriten ⇒
      logger.debug(s"state: CLOSED, sender: $sender, message: ${SockJsSession.Register}, openWriten: $openWriten")
      (register andThen sendOpenMessage andThen resetListener)(sender)

    case SockJsSession.Register ⇒
      logger.debug(s"state: CLOSED, sender: $sender, message: ${SockJsSession.Register}, openWriten: $openWriten")
      (register andThen sendCloseMessage andThen resetListener)(sender)
  }

  def createAndRegister(props: Props, name: String, request: RequestHeader) = {
    val t = context.actorOf(props, name + transportId)
    t ! SockJsSession.Register
    sender ! t
    handler ! request
    transportId += 1
  }

  val register = (tl: ActorRef) ⇒ {
    if (transportListener.isEmpty || transportListener.get == tl) {
      transportListener = Some(tl)
    } else {
      tl ! SockJsSession.Close(2010, "Another connection still open")
      logger.debug(s"Refuse transport, Another connection still open")
    }
    tl
  }: ActorRef

  val sendOpenMessage = (tl: ActorRef) ⇒ {
    tl ! SockJsSession.OpenMessage
    openWriten = true
    tl
  }: ActorRef

  val resetListener = (tl: ActorRef) ⇒ {
    play.api.Logger.debug("reset LISTENER")
    //tl ! PoisonPill
    transportListener = None
    setTimer()
  }: Unit

  val becomeOpen = (_: Unit) ⇒ {
    play.api.Logger.debug("-> become OPEN")
    context become (open orElse timeout)
    startHeartBeat()
  }: Unit

  val becomeClosed = (_: Unit) ⇒ {
    play.api.Logger.debug("-> become CLOSED")

    context become (closed orElse timeout)
    upChannel.eofAndEnd()
  }: Unit

  val writePendingMessages = (tl: ActorRef) ⇒ {
    //TODO: unify writes
    logger.debug(s"writePendingMessages: tl: $tl, pendingWrites: $pendingWrites")
    tl ! SockJsSession.Message("a" + JsonCodec.encodeJson(JsArray(pendingWrites.dequeueAll(_ ⇒ true).toList)))
    tl
  }: ActorRef

  val sendCloseMessage = (tl: ActorRef) ⇒ {
    tl ! closeMessage
    tl
  }: ActorRef

  val sendHeartBeatMessage = (tl: ActorRef) ⇒ {
    tl ! SockJsSession.HeartBeat
    tl
  }: ActorRef

  // another pending message
  def enqueue(msg: JsValue) {
    //TODO: check the queue size
    logger.debug(s"enqueue msg: $msg, pendingWrites: $pendingWrites")
    pendingWrites += msg
  }

  def handleMessages(msgs: JsValue) {
    msgs match {
      case msg: JsArray ⇒ msg.value.foreach(m ⇒ upChannel push m)
      case msg: JsValue ⇒ upChannel push msg
    }
  }

  def setTimer() {
    import scala.language.postfixOps
    for (t ← timer) t.cancel()
    timer = Some(context.system.scheduler.scheduleOnce(6 seconds, self, SockJsSession.Timeout)) // The only case where the session is actually closed
  }

  def startHeartBeat() {
    //TODO: heartbeat need test
    import scala.language.postfixOps
    heartBeatTask = Some(context.system.scheduler.schedule(heartBeatPeriod milliseconds, heartBeatPeriod milliseconds, self, SockJsSession.HeartBeat))
  }

  def doClose() {
    //logger.debug("Session is going to shutdown")
    self ! PoisonPill
    handler ! PoisonPill
    for (tl ← transportListener) tl ! PoisonPill
    for (h ← heartBeatTask) h.cancel()
    for (t ← timer) t.cancel()
  }
}

object SockJsSession {

  // JSClient send
  case class Incoming(msg: JsValue)

  // register the transport actor and holds for the next message to write to the JSClient
  case object Register

  // creates a child transport actor and registers it
  case class CreateAndRegister(props: Props, name: String, request: RequestHeader)

  case object Unregister

  case object OpenMessage

  case class Message(msg: String)

  case class Close(code: Int, reason: String)

  case object HeartBeat

  case class Outgoing(msg: JsValue)

  object Timeout

}