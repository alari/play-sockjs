package mirari.sockjs.impl

import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.actor._
import play.api.mvc.RequestHeader
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import mirari.sockjs.impl.Transport.{StreamingFinished, ConnectionInterrupted}

/**
 * @author alari
 * @since 12/13/13
 */
abstract class Transport extends Actor {

  import Session._

  def receive = {
    case RegisterTransport =>
      register()

    case ConnectionInterrupted =>
      context.parent ! UnregisterTransport
      self ! PoisonPill

    case StreamingFinished =>
      context.parent ! UnregisterTransport


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

class ChannelTransport(channel: Future[Concurrent.Channel[String]], initialPayload: String = null, frameFormatter: String => String = a => a, maxStreamingBytes: Int = 127000) extends Transport {
  var bytesSent = 0

  val InitialPayloadDelay = FiniteDuration(10, "milliseconds")

  override def register() {
    if (initialPayload != null) {
      channel.map {
        c =>
          c.push(initialPayload)
          // We need to be sure that payload have been sent before any frame
          super.register()
      }
    } else super.register()
  }

  def sendFrame(frame: String) = {
    val msg = frameFormatter(frame)
    channel.map(_.push(msg))
    bytesSent += msg.length
    if(bytesSent >= maxStreamingBytes) {
      channel.map(_.eofAndEnd())
      false
    } else true
  }
}

class PromiseTransport(promise: Promise[String]) extends Transport {
  def sendFrame(frame: String) = {
    promise.success(frame)
    false
  }
}

object Transport {

  import SockJs.Timeout

  def fullDuplex(session: ActorRef)(implicit request: RequestHeader): Future[FullDuplex] = {
    val p = Promise[Concurrent.Channel[String]]()

    withTransport(session, new ChannelTransport(p.future)) {
      transport =>
        val out = Concurrent.unicast[String]({
          c => p.success(c)
        }, {
          transport ! StreamingFinished
        })
        val in = Iteratee.foreach[String] {
          s =>
            incomingFrame(s, session)
        } map {
          _ =>
            transport ! ConnectionInterrupted
        }
        FullDuplex(out, in)
    }
  }

  def halfDuplex(session: ActorRef, initialPayload: String = null, frameFormatter: String => String = a => a, maxStreamingBytes: Int = 127000)(implicit request: RequestHeader): Future[HalfDuplex] = {
    val p = Promise[Concurrent.Channel[String]]()

    withTransport(session, new ChannelTransport(p.future, initialPayload, frameFormatter, maxStreamingBytes)) {
      transport =>
        val out = Concurrent.unicast[String]({
          c => p.success(c)
        }, {
          transport ! StreamingFinished
        })

        HalfDuplex(out)
    }
  }

  def singleFramePlex(session: ActorRef)(implicit request: RequestHeader): Future[SingleFramePlex] = {
    val p = Promise[String]()
    withTransport(session, new PromiseTransport(p)) {
      _ =>
        SingleFramePlex(p.future)
    }
  }

  private def withTransport[T](session: ActorRef, transportBuilder: => Transport)(f: ActorRef => T)(implicit request: RequestHeader): Future[T] = {
    import akka.pattern.ask
    session ? Session.CreateTransport(Props({
      transportBuilder
    }), request) map {
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

  case object ConnectionInterrupted

  case object StreamingFinished

}

trait SockJsTransports {

  import Transport._

  val MaxBytesSent = 127000

  protected def xhrPollingTransport(session: ActorRef)(implicit request: RequestHeader, ctx: ExecutionContext) =
    singleFramePlex(session).flatMap {
      _.out.map(Frames.Format.xhr)
    }

  protected def xhrStreamingTransport(session: ActorRef, maxBytesSent: Int = MaxBytesSent)(implicit request: RequestHeader) =
    halfDuplex(session, Frames.Prelude.xhrStreaming, Frames.Format.xhr, maxBytesSent).map {
      _.out
    }

  protected def eventsourceTransport(session: ActorRef, maxBytesSent: Int = MaxBytesSent)(implicit request: RequestHeader) =
    halfDuplex(session, Frames.Prelude.eventsource, Frames.Format.eventsource, maxBytesSent).map {
      _.out
    }

  protected def htmlfileTransport(session: ActorRef, callback: String, maxBytesSent: Int = MaxBytesSent)(implicit request: RequestHeader) =
    halfDuplex(session, Frames.Prelude.htmlfile(callback), Frames.Format.htmlfile, maxBytesSent).map {
      _.out
    }

  protected def jsonpTransport(session: ActorRef, callback: String)(implicit request: RequestHeader, ctx: ExecutionContext) =
    singleFramePlex(session).flatMap {
      _.out.map(Frames.Format.jsonp(callback))
    }

  protected def websocketTransport(session: ActorRef)(implicit request: RequestHeader) = fullDuplex(session).map {
    t =>
      (t.in, t.out)
  }
}