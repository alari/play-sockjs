package mirari.sockjs.impl

import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import scala.concurrent.{Future, Promise}
import akka.actor._
import play.api.mvc.RequestHeader
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
 * @author alari
 * @since 12/13/13
 */
abstract class Transport extends Actor {

  import Session._

  def receive = {
    case RegisterTransport =>
      register()

    case OutgoingRaw(frame) =>
      if (sendFrame(frame)) context.parent ! RegisterTransport
      else self ! PoisonPill
  }

  def register() {
    context.parent ! RegisterTransport
  }

  // is it capable to send another frame after this one or not
  def sendFrame(frame: String): Boolean
}

class ChannelTransport(channel: Concurrent.Channel[String], initialPayload: String = null, frameFormatter: String => String = a => a, maxStreamingBytes: Int = 127000) extends Transport {
  var bytesSent = 0

  val InitialPayloadDelay = FiniteDuration(10, "milliseconds")

  override def register() {
    if(initialPayload != null) {
      play.api.Logger.error("FIXME: payload is sent only when this logging line is there "+initialPayload)
      channel.push(initialPayload)
        super.register()
    } else super.register()
  }

  def sendFrame(frame: String) = {
    val msg = frameFormatter(frame)
    channel.push(msg)
    bytesSent += msg.length
    bytesSent < maxStreamingBytes
  }
}

class PromiseTransport(promise: Promise[String]) extends Transport {
  def sendFrame(frame: String) = {
    promise.success(frame)
    false
  }
}

object Transport {
  implicit val Timeout = akka.util.Timeout(100)

  def fullDuplex(session: ActorRef)(implicit request: RequestHeader): Future[FullDuplex] = {
    val (out, channel) = outChannel()
    withTransport(session, new ChannelTransport(channel)) {
      transport =>
        val in = Iteratee.foreach[String] {
          s =>
            incomingFrame(s, session)
        } map {
          _ =>
            transport ! PoisonPill
        }
        FullDuplex(out, in)
    }
  }

  def halfDuplex(session: ActorRef, initialPayload: String = null, frameFormatter: String => String = a => a, maxStreamingBytes: Int = 127000)(implicit request: RequestHeader): Future[HalfDuplex] = {
    val (out, channel) = outChannel()
    withTransport(session, new ChannelTransport(channel, initialPayload, frameFormatter, maxStreamingBytes)) {
      _ =>
        HalfDuplex(out &> Concurrent.buffer(maxStreamingBytes / 10))
    }
  }

  def singleFramePlex(session: ActorRef)(implicit request: RequestHeader): Future[SingleFramePlex] = {
    val p = Promise[String]()
    withTransport(session, new PromiseTransport(p)) {
      _ =>
        SingleFramePlex(p.future)
    }
  }

  private def outChannel(): (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]

  private def withTransport[T](session: ActorRef, transportBuilder: => Transport)(f: ActorRef => T)(implicit request: RequestHeader): Future[T] = {
    import akka.pattern.ask
    session ? Session.CreateTransport(Props({transportBuilder}), request) map {
      case transport: ActorRef =>
        f(transport)

      case _ =>
        play.api.Logger.error("Cannot create transport")
        throw new RuntimeException
    }
  }

  def incomingFrame(message: String, session: ActorRef): Unit = session ! Session.Incoming(message)

  case class FullDuplex(out: Enumerator[String], in: Iteratee[String, Unit])

  case class HalfDuplex(out: Enumerator[String])

  case class SingleFramePlex(out: Future[String])
}

