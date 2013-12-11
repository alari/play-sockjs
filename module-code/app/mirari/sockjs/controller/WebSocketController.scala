package mirari.sockjs.controller

import play.api.mvc.{WebSocket, Controller}
import play.api.libs.iteratee.{Enumerator, Input, Done}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import mirari.sockjs.transport.TransportController


/**
 * @author alari
 * @since 12/10/13
 */
object WebSocketController extends TransportController{

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


              play.api.Logger.debug("websocket found")
            stopSocket
            case None =>
            stopSocket
          }
      }
  }

  def stopSocket = {
    val iteratee = Done[String, Unit]((), Input.EOF)
    val enumerator = Enumerator[String]("c[0,\"stop\"]") >>> Enumerator.enumInput(Input.EOF)

    Future.successful((iteratee, enumerator))
  }
}
