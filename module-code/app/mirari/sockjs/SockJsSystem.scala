package mirari.sockjs

import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.libs.json.Json
import scala.util.Random
import mirari.sockjs.service.SockJsService
import mirari.sockjs.handler.Handler

/**
 * @author alari
 * @since 12/10/13
 */
object SockJsSystem {
  lazy val system = ActorSystem("mirari-sockjs")

  val services = collection.mutable.Map[String, ActorRef]()

  def initService(name: String, handler: Class[_ <: Handler], websocket: Boolean = true, cookieNeeded: Boolean = true) {
    services(name) = system.actorOf(Props(classOf[SockJsService], handler, websocket, cookieNeeded), name)
  }

  def service(name: String) = services.get(name)

  case object RequestInfo

  case class Info(websocket: Boolean,
                  cookie_needed: Boolean,
                  origins: Seq[String] = Seq("*:*"),
                  entropy: Long = Random.nextLong())

  object Info {
    implicit val writes = Json.writes[Info]
  }

}