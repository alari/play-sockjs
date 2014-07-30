package infra.sockjs

import play.api.test.{WithApplication, PlaySpecification}
import akka.actor.{ActorRef, Props}
import akka.pattern.ask

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 2:22
 */
class EchoServiceSpec extends PlaySpecification {
  "sockjs system" should {
    "register echo service and create a session" in new WithApplication {

      val service = SockJs.system.actorOf(Props(new Service(Service.Params(Props(new SockJsHandler.Echo), 200, 100))))

      service must beAnInstanceOf[ActorRef]

      (service ? Service.SessionExists("1")).mapTo[Boolean] must beFalse.await
      (service ? Service.RetrieveSession("1")).mapTo[Option[ActorRef]] must beNone.await

      (service ? Service.CreateAndRetrieveSession("1")).mapTo[ActorRef] must beAnInstanceOf[ActorRef].await


      (service ? Service.SessionExists("1")).mapTo[Boolean] must beTrue.await
      (service ? Service.RetrieveSession("1")).mapTo[Option[ActorRef]] must beSome[ActorRef].await

      (service ? Service.CreateAndRetrieveSession("1")).mapTo[ActorRef] must beAnInstanceOf[ActorRef].await

    }
  }
}
