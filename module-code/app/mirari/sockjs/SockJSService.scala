package mirari.sockjs

import akka.actor.{Props, Actor}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.http.{Writeable, ContentTypeOf}
import akka.pattern.{ask, pipe}


/**
 * @author alari
 * @since 12/9/13
 */
trait SockJSService extends Actor {

  import SockJSService._
  import SockJSSessionActor._

  implicit val Timeout = SockJS.Timeout
  import context.dispatcher

  def info: Info

  def receive = {
    case InfoRequest =>
      sender ! info

    case SessionMessage(session, msg) =>
      (context.child(session) match {
        case Some(a) => a
        case None =>
          context.actorOf(sessionProps(session), session)
      }) ! msg

    case RetrieveSessionMessages(session) =>
      val s = sender
      (context.child(session) match {
        case Some(a) =>
          a
        case None =>
          context.actorOf(sessionProps(session), session)
      }) ? OpenConnection pipeTo s

  }

  def getSessionActor(session: String) = context.child(session)

  def sessionProps(session: String): Props
}

object SockJSService {

  case class RetrieveSessionMessages(session: String)

  case object InfoRequest

  case class Info(websocket: Boolean,
                  cookie_needed: Boolean,
                  origins: Seq[String] = Seq("*:*"),
                  entropy: Long = (Math.random() * 100000).toLong)

  object Info {
    implicit val writes = Json.writes[Info]
  }

  abstract sealed class Frame(code: Char) {
    protected def content: String = ""

    override def toString = content + s"$code$content\n"
  }

  object Frame {
    implicit val ct: ContentTypeOf[Frame] = ContentTypeOf[mirari.sockjs.SockJSService.Frame](Some("application/json"))
    implicit val writeable = Writeable[Frame] {
      f: Frame => f.toString.getBytes
    }
  }

  case object OpenFrame extends Frame('o')

  case object CloseFrame extends Frame('c')

  case object CloseGoAway extends Frame('c') {
    override def content = "[3000,\"Go away!\"]"
  }

  case object HeartbeatFrame extends Frame('h')

  case class ArrayFrame(messages: Seq[JsValue]) extends Frame('a') {
    override def content = JsArray(messages).toString()
  }

  case class SessionMessage(session: String, msg: Any)

}