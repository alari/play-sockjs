package mirari.sockjs

import akka.actor.{ActorRef, Props, ActorSystem}
import scala.concurrent.Future
import akka.pattern.ask

/**
 * @author alari
 * @since 12/13/13
 */
object SockJs {
  implicit val system = ActorSystem("sockjs")
  implicit val Timeout = akka.util.Timeout(100)

  val services = system.actorOf(Props[Services], "services")

  def registerService(service: String, params: Service.Params): Future[ActorRef] =
    (services ? Services.Register(service, params)).mapTo[ActorRef]

  def checkSession(service: String, id: String): Future[Boolean] =
    (services ? Services.SessionExists(service, id)).mapTo[Boolean]

  def retrieveSession(service: String, id: String): Future[Option[ActorRef]] =
    (services ? Services.RetrieveSession(service, id)).mapTo[Option[ActorRef]]

  def createSession(service: String, id: String): Future[ActorRef] =
    (services ? Services.CreateAndRetrieveSession(service, id)).mapTo[ActorRef]
}
