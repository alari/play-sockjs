package mirari.sockjs.controller

import play.api.mvc.{WebSocket, Controller}
import mirari.sockjs.SockJS
import play.api.libs.iteratee.{Enumerator, Input, Done}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global


/**
 * @author alari
 * @since 12/10/13
 */
object WebSocketController extends Controller{

  def rootWebsocket(service: String) = WebSocket.async[String] {
    request =>
    stopSocket
  }

  def websocket(service: String, server: String, session: String) = WebSocket.async[String] {
    request =>
      request.headers.get(CONNECTION) match {
        case Some("close") =>
          stopSocket
        case _ =>
          SockJS.openSocket(service, session)
      }
  }

  def stopSocket = {
    val iteratee = Done[String, Unit]((), Input.EOF)
    val enumerator = Enumerator[String]("c[0,\"stop\"]") >>> Enumerator.enumInput(Input.EOF)

    Future.successful((iteratee, enumerator))
  }
}
