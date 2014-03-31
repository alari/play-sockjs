package mirari.sockjs

import akka.actor.{ActorRef, Props, ActorSystem}
import scala.concurrent.Future
import akka.pattern.ask
import play.api.Plugin

/**
 * @author alari
 * @since 12/13/13
 */
object SockJs {
  def plugin = play.api.Play.current.plugin[SockJs].getOrElse(throw new IllegalStateException("SockJs plugin is not enabled!"))

  implicit def system = plugin.system

  implicit def Timeout = plugin.Timeout

  def registerService(service: String, params: Service.Params): Future[ActorRef] =
    plugin.registerService(service, params)

  def checkSession(service: String, id: String): Future[Boolean] =
    plugin.checkSession(service, id)

  def retrieveSession(service: String, id: String): Future[Option[ActorRef]] =
    plugin.retrieveSession(service, id)

  def createSession(service: String, id: String): Future[ActorRef] =
    plugin.createSession(service, id)
}

class SockJs(app: play.api.Application) extends Plugin {
  @volatile private var actorSystem: Option[ActorSystem] = None
  @volatile private var servicesRef: Option[ActorRef] = None

  private val Timeout = akka.util.Timeout(app.configuration.getInt("sockjs.timeout").getOrElse(100))

  implicit def system: ActorSystem = actorSystem.getOrElse{
    play.api.Logger.warn("[sockjs] Launching ActorSystem on demand")
    launchSystem()
    actorSystem.getOrElse(throw new IllegalStateException("Cannot launch ActorSystem"))
  }

  def services: ActorRef = servicesRef.getOrElse{
    play.api.Logger.warn("[sockjs] Launching Services on demand")
    launchSystem()
    servicesRef.getOrElse(throw new IllegalStateException("Services Actor is None"))
  }

  override def onStart() {
    launchSystem()
    play.api.Logger.debug("[sockjs] system is up and running")

    super.onStart()
  }

  private def launchSystem() {
    if(actorSystem.isDefined && servicesRef.isDefined) {
      play.api.Logger.debug("[sockjs] system is already launched")
    } else {
      play.api.Logger.debug("[sockjs] launching system")
      synchronized {
        actorSystem = Some(ActorSystem(app.configuration.getString("sockjs.system").getOrElse("sockjs")))
        servicesRef = actorSystem.map(_.actorOf(Props[Services], "services"))
      }
    }
  }

  private def stopSystem() {
    synchronized {
      servicesRef.map(s => actorSystem.map(_.stop(s)))
      servicesRef = None
      actorSystem.map(_.shutdown())
      actorSystem = None
    }
  }

  override def onStop() {
    play.api.Logger.debug("[sockjs] stopping system")
    stopSystem()
    super.onStop()
  }

  def registerService(service: String, params: Service.Params)(implicit timeout: akka.util.Timeout = Timeout): Future[ActorRef] =
    (services ? Services.Register(service, params)).mapTo[ActorRef]

  def checkSession(service: String, id: String)(implicit timeout: akka.util.Timeout = Timeout): Future[Boolean] =
    (services ? Services.SessionExists(service, id)).mapTo[Boolean]

  def retrieveSession(service: String, id: String)(implicit timeout: akka.util.Timeout = Timeout): Future[Option[ActorRef]] =
    (services ? Services.RetrieveSession(service, id)).mapTo[Option[ActorRef]]

  def createSession(service: String, id: String)(implicit timeout: akka.util.Timeout = Timeout): Future[ActorRef] =
    (services ? Services.CreateAndRetrieveSession(service, id)).mapTo[ActorRef]
}