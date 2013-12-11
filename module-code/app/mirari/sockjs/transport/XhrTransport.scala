package mirari.sockjs.transport

/**
 * @author alari
 * @since 12/10/13
 */

import scala.Array.canBuildFrom
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt
import akka.actor.{ActorRef, PoisonPill, Props, actorRef2Scala}
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.Json
import com.fasterxml.jackson.core.JsonParseException
import mirari.sockjs.service.SockJsSession
import mirari.sockjs.frames.{SockJsFrames, JsonCodec}

class XhrPollingActor(promise: Promise[String]) extends TransportActor {
  def sendFrame(msg: String): Boolean = {
    play.api.Logger.info(s"SENDING FRAME $msg")
    promise success msg + "\n"
    false
  }

  override def postStop() {
    //context.parent ! SockJsSession.Close(1002, "Connection interrupted") //FIXME: this should be closing the session but it works fine by mistake!!!!
    play.api.Logger.debug("xhr polling poststop")
    context.parent ! SockJsSession.Unregister
    super.postStop()
  }
}

class XhrStreamingActor(channel: Concurrent.Channel[Array[Byte]], maxBytesStreaming: Int)
  extends TransportActor {
  var bytesSent = 0

  override def preStart() {
    import scala.language.postfixOps
    context.system.scheduler.scheduleOnce(100 milliseconds) {
      channel push SockJsFrames.XHR_STREAM_H_BLOCK
      session ! SockJsSession.Register
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
    session ! SockJsSession.Close(1002, "Connection interrupted")
    super.postStop()
  }
}


import play.api.mvc.Action
import concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask


/**
 * @author alari
 * @since 12/10/13
 */
object XhrController extends TransportController {
  val maxBytesStreaming = 4096
  val maxLength = 102400


  def xhr(service: String, server: String, session: String) = Action.async {
    implicit request =>
      play.api.Logger.debug("_____xhr_polling = "+session)
      withSessionFlat(service, session) {
        ss =>
          play.api.Logger.debug(ss.toString())
          val promise = Promise[String]()
        val props = Props(classOf[XhrPollingActor], promise)
        play.api.Logger.debug("we are there")
          ss ! SockJsSession.CreateAndRegister(props, "xhr_polling")
          promise.future.map {
            m ⇒
              Ok(m.toString)
                .withHeaders(
                  CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*)
          }
      }
  }

  def xhrOpts(service: String, server: String, session: String) = Action {
    implicit request =>
      handleCORSOptions("OPTIONS", "POST")
  }

  def xhr_send(service: String, server: String, session: String) = Action.async(parse.anyContent) {
    implicit request =>
      withExistingSession(service, session) {
        ss =>
          val message: String = request.body.asRaw.flatMap(r ⇒ r.asBytes(maxLength).map(b ⇒ new String(b)))
            .getOrElse(request.body.asText
            .orElse(request.body.asJson map Json.stringify)
            .getOrElse(""))
          if (message == "") {
            play.api.Logger.error(s"xhr_send error: couldn't read the body, content-type: ${request.contentType}")
            InternalServerError("Payload expected.")
          } else
            try {
              val contentType = Transport.CONTENT_TYPE_PLAIN
              println(s"XHR Send -->>>>>:::: $message, decoded message: ${JsonCodec.decodeJson(message)}")
              ss ! SockJsSession.Incoming(JsonCodec.decodeJson(message))
              NoContent
                .withHeaders(
                  CONTENT_TYPE -> contentType,
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*)
            } catch {
              case e: JsonParseException ⇒
                play.api.Logger.debug(s"xhr_send, error in parsing message, errorMessage: ${e.getMessage}")
                InternalServerError("Broken JSON encoding.")
            }
      }
  }

  def xhr_sendOpts(service: String, server: String, session: String) = Action {
    implicit request =>
      handleCORSOptions("OPTIONS", "POST")
  }

  def xhr_streaming(service: String, server: String, session: String) = Action.async {
    implicit request =>
      withExistingSessionFlat(service, session) {
        ss =>
          val (enum, channel) = Concurrent.broadcast[Array[Byte]]
          ss ? SockJsSession.CreateAndRegister(Props(new XhrStreamingActor(channel, maxBytesStreaming)), "xhr_streaming") map {
            case transport: ActorRef =>
              Ok.chunked(enum.onDoneEnumerating(transport ! PoisonPill))
                .withHeaders(
                  CONTENT_TYPE -> "application/javascript;charset=UTF-8",
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*)
            case _ =>
              InternalServerError
          }
      }
  }

  def xhr_streamingOpts(service: String, server: String, session: String) = Action {
    implicit request =>
      handleCORSOptions("OPTIONS", "POST")
  }
}
