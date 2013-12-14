package mirari.sockjs.impl

import akka.actor.{Props, Actor}
import scala.util.Random
import play.api.libs.json.Json

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 1:50
 */
class Service(params: Service.Params) extends Actor {

  import Service._

  def info() = Info(params.websocket, params.cookie_needed)

  def receive = {
    case Info =>
      sender ! info()

    case SessionExists(id) =>
      sender ! context.child(id).isDefined

    case RetrieveSession(id) =>
      sender ! context.child(id)

    case CreateAndRetrieveSession(id) =>
      context.child(id) match {
        case Some(s) => sender ! s
        case None =>
          sender ! context.actorOf(Props(new Session(params.handlerProps, timeoutMs = params.timeoutMs, heartbeatPeriodMs = params.heartbeatPeriodMs)), id)
      }
  }
}

object Service {

  case class RetrieveSession(id: String)

  case class CreateAndRetrieveSession(id: String)

  case class SessionExists(id: String)

  private def randomNumber() = 2L << 30 + Random.nextInt

  case class Params(
                     websocket: Boolean,
                     cookie_needed: Boolean,
                     handlerProps: Props,
                     timeoutMs: Int = SockJs.SessionTimeoutMs,
                     heartbeatPeriodMs: Int = SockJs.SessionHeartbeatMs
                     )

  case class Info(websocket: Boolean,
                  cookie_needed: Boolean,
                  origins: Seq[String] = Seq("*:*"),
                  entropy: Long = randomNumber())

  object Info {
    implicit val writes = Json.writes[Info]
  }

}
