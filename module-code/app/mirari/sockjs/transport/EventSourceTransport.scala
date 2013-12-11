package mirari.sockjs.transport

import play.api.mvc.Action
import play.api.libs.iteratee.Concurrent
import com.cloud9ers.play2.sockjs.Session
import akka.actor.{ActorRef, Props}
import mirari.sockjs.service.SockJsSession
import play.api.libs.EventSource
import concurrent.ExecutionContext.Implicits.global

/**
 * @author alari
 * @since 12/11/13
 */
class EventSourceTransport(channel: Concurrent.Channel[String], maxBytesStreaming: Int) extends TransportActor {
  var bytesSent = 0


  override def doRegister() {
    import scala.language.postfixOps
    session ! Session.Register
  }

  def sendFrame(m: String): Boolean = {
    //val msg = s"data: $m\r\n\r\n"
    bytesSent += m.length
    println("EventSource ::<<<<<<<<< " + m)
    channel push m
    if (bytesSent < maxBytesStreaming)
      true
    else {
      channel.eofAndEnd()
      false
    }
  }
}

object EventSourceController extends TransportController {

  import akka.pattern.ask

  def eventsource(service: String, server: String, session: String) = Action.async {
    implicit request =>
      withExistingSessionFlat(service, session) {
        ss =>
          val (enum, channel) = Concurrent.broadcast[String]

          ss ? SockJsSession.CreateAndRegister(Props(new EventSourceTransport(channel, 127000)), "eventsource") map {
            case transport: ActorRef =>
              Ok.chunked(
                enum &> EventSource()
              ).as("text/event-stream").withHeaders(
                  CONTENT_TYPE -> "text/event-stream;charset=UTF-8",
                  CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0")
                .withHeaders(cors: _*)
          }
      }
  }
}