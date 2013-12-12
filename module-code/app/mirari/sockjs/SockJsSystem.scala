package mirari.sockjs

import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.libs.json.Json
import scala.util.Random
import mirari.sockjs.service.SockJsService
import play.api.Mode

/**
 * @author alari
 * @since 12/10/13
 */
object SockJsSystem {
  lazy val system = ActorSystem("mirari-sockjs")

  private[this] var services = Map[String, ActorRef]()

  def initService(name: String, handler: Props, websocket: Boolean = true, cookieNeeded: Boolean = true) {
    if(!services.contains(name))
      services += name -> system.actorOf(Props(new SockJsService(handler, websocket, cookieNeeded)), name)
    else if(play.api.Play.current.mode != Mode.Test)
      play.api.Logger.error("Trying to init service `" + name + "`, but it has already been initiated!")
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