package mirari.sockjs.impl

import play.api.test.PlaySpecification
import akka.actor.{ActorRef, Props}

/**
 * @author alari (name.alari@gmail.com)
 * @since 15.12.13 2:22
 */
class EchoServiceSpec extends PlaySpecification{
  "sockjs system" should {
    "register echo service and create a session" in {
      SockJs.getInfo("echo") must throwAn[Exception].await

      SockJs.registerService("echo", Props(new SockJsHandler.Echo)) must beAnInstanceOf[ActorRef].await

      SockJs.getInfo("echo") must beAnInstanceOf[Service.Info].await

      SockJs.checkSession("echo", "1") must beFalse.await
      SockJs.retrieveSession("echo", "1") must beNone.await

      SockJs.createSession("echo", "1") must beAnInstanceOf[ActorRef].await

      SockJs.checkSession("echo", "1") must beTrue.await
      SockJs.retrieveSession("echo", "1") must beSome[ActorRef].await
      SockJs.createSession("echo", "1") must beAnInstanceOf[ActorRef].await
    }
  }
}
