package mirari.sockjs.impl

import play.api.libs.iteratee.{Iteratee, Enumerator, Concurrent}
import scala.concurrent.{Future, Promise}
import akka.actor._
import play.api.mvc.RequestHeader

/**
 * @author alari
 * @since 12/13/13
 */
abstract class Transport extends Actor {

  import Session._

  def receive = {
    case RegisterTransport =>
      // possibly we may do something before triggering register
      context.parent ! RegisterTransport

    case OutgoingRaw(frame) =>
      if (sendFrame(frame)) context.parent ! RegisterTransport
  }

  // is it capable to send another frame after this one or not
  def sendFrame(frame: String): Boolean
}

class ChannelTransport(channel: Concurrent.Channel[String]) extends Transport {
  def sendFrame(frame: String) = {
    channel.push(frame)
    true
  }
}

class PromiseTransport(promise: Promise[String]) extends Transport {
  def sendFrame(frame: String) = {
    promise.success(frame)
    false
  }
}

object Transport {
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

  def halfDuplex(session: ActorRef)(implicit request: RequestHeader): Future[HalfDuplex] = {
    val (out, channel) = outChannel()
    withTransport(session, new ChannelTransport(channel)) {
      _ =>
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