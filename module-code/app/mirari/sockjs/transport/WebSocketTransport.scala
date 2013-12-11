package mirari.sockjs.transport

import play.api.libs.iteratee._
import play.api.mvc.WebSocket
import scala.Some
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import mirari.sockjs.service.SockJsSession
import mirari.sockjs.frames.JsonCodec
import akka.actor.{PoisonPill, ActorRef, Props}


/**
 * @author alari
 * @since 12/11/13
 */
class WebSocketTransport(outChannel: Concurrent.Channel[String]) extends TransportActor{
  def sendFrame(m: String) = {
    play.api.Logger.debug("\n:-:-:-:-\n"+m+"\n\n")
    outChannel push m
    true
  }
}

object WebSocketController extends TransportController{

  import akka.pattern.ask

  def rootWebsocket(service: String) = WebSocket.async[String] {
    implicit request =>

      stopSocket
  }

  def websocket(service: String, server: String, session: String) = WebSocket.async[String] {
    implicit request =>
      request.headers.get(CONNECTION) match {
        case Some("close") =>
          stopSocket
        case _ =>
          getOrCreateSession(service, session).flatMap {
            case Some(ss) =>
              val (out, outChannel) = Concurrent.broadcast[String]

              ss ? SockJsSession.CreateAndRegister(Props(new WebSocketTransport(outChannel)), "websocket") map {
                case transport: ActorRef =>
                  val in = Iteratee.foreach[String](s => ss ! SockJsSession.Incoming(JsonCodec.decodeJson(s))) map {
                    _ =>
                    // Kill this actor when connection is broken
                      transport ! PoisonPill
                  }
                  (in, out)
                case _ =>
                  stopSocketPlain
              }
            case None =>
              stopSocket
          }
      }
  }

  def stopSocket = {
    Future.successful(stopSocketPlain)
  }

  def stopSocketPlain = {
    val iteratee = Done[String, Unit]((), Input.EOF)
    val enumerator = Enumerator[String]("c[0,\"stop\"]") >>> Enumerator.enumInput(Input.EOF)

    (iteratee, enumerator)
  }
}
