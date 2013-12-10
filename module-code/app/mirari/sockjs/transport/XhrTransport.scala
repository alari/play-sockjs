package mirari.sockjs.transport

/**
 * @author alari
 * @since 12/10/13
 */
import scala.Array.canBuildFrom
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt
import com.cloud9ers.play2.sockjs.{ JsonCodec, Session, SockJsFrames }
import akka.actor.{ ActorRef, PoisonPill, Props, actorRef2Scala }
import play.api.libs.iteratee.Concurrent
import play.api.mvc.{ AnyContent, Request, Result }
import play.api.libs.json.Json
import com.fasterxml.jackson.core.JsonParseException
import com.cloud9ers.play2.sockjs.transports.{TransportController, Transport}

class XhrPollingActor(promise: Promise[String], session: ActorRef) extends TransportActor(session) {
  session ! Session.Register
  def sendFrame(msg: String): Boolean = {
    promise success msg + "\n"
    false
  }

  override def postStop() {
    session ! Session.Close(1002, "Connection interrupted")  //FIXME: this should be closing the session but it works fine by mistake!!!!
    super.postStop()
  }
}

class XhrStreamingActor(channel: Concurrent.Channel[Array[Byte]], session: ActorRef, maxBytesStreaming: Int)
  extends TransportActor(session) {
  var bytesSent = 0
  override def preStart() {
    import scala.language.postfixOps
    context.system.scheduler.scheduleOnce(100 milliseconds) {
      channel push SockJsFrames.XHR_STREAM_H_BLOCK
      session ! Session.Register
    }
  }

  def sendFrame(msg: String) = {
    val bytes = s"$msg\n".toArray.map(_.toByte)
    bytesSent += bytes.length
    channel push bytes
    if (bytesSent < maxBytesStreaming)
      true
    else {
      channel.eofAndEnd()
      false
    }
  }

  override def postStop() {
    channel push Array[Byte]()
    session ! Session.Close(1002, "Connection interrupted")
    super.postStop()
  }
}

object XhrTransport extends TransportController {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def xhrPolling(sessionId: String, session: ActorRef)(implicit request: Request[AnyContent]) = Async {
    val promise = Promise[String]()
    system.actorOf(Props(new XhrPollingActor(promise, session)), s"xhr-polling.$sessionId.$randomNumber")
    promise.future.map { m ⇒
      Ok(m.toString)
        .withHeaders(
          CONTENT_TYPE -> "application/javascript;charset=UTF-8",
          CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
        .withHeaders(cors: _*)
    }
  }

  def xhrStreaming(sessionId: String, session: ActorRef)(implicit request: Request[AnyContent]): Result = {
    val (enum, channel) = Concurrent.broadcast[Array[Byte]]
    val xhrStreamingActor = system.actorOf(Props(new XhrStreamingActor(channel, session.asInstanceOf[ActorRef], maxBytesStreaming)), s"xhr-streaming.$sessionId.$randomNumber")
    Ok.chunked(enum.onDoneEnumerating(xhrStreamingActor ! PoisonPill))
      .withHeaders(
        CONTENT_TYPE -> "application/javascript;charset=UTF-8",
        CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
      .withHeaders(cors: _*)
  }

  def xhrSend(sessionId: String, session: ActorRef)(implicit request: Request[AnyContent]): Result = {
    //FIXME: if the content-type is text/xml then play will return 400 bad request before it comes here WTF! :@
    val message: String = request.body.asRaw.flatMap(r ⇒ r.asBytes(maxLength).map(b ⇒ new String(b)))
      .getOrElse(request.body.asText
      .orElse(request.body.asJson map Json.stringify)
      .getOrElse(""))
    if (message == "") {
      logger.error(s"xhr_send error: couldn't read the body, content-type: ${request.contentType}")
      InternalServerError("Payload expected.")
    } else
      try {
        val contentType = Transport.CONTENT_TYPE_PLAIN
        println(s"XHR Send -->>>>>:::: $message, decoded message: ${JsonCodec.decodeJson(message)}")
        session ! Session.Send(JsonCodec.decodeJson(message))
        NoContent
          .withHeaders(
            CONTENT_TYPE -> contentType,
            CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
          .withHeaders(cors: _*)
      } catch {
        case e: JsonParseException ⇒
          logger.debug(s"xhr_send, error in parsing message, errorMessage: ${e.getMessage}")
          InternalServerError("Broken JSON encoding.")
      }
  }
}

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global


/**
 * @author alari
 * @since 12/10/13
 */
object XhrController extends Controller{
  def xhr(service: String, server: String, session: String) = Action.async {
    Future(NotImplemented)
  }

  def xhrOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_send(service: String, server: String, session: String) = Action.async(parse.anyContent) {
    request =>
      Future(NotImplemented)
  }

  def xhr_sendOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streaming(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streamingOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }
}
