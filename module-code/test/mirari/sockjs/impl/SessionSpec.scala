package mirari.sockjs.impl

import play.api.test.PlaySpecification
import akka.actor._
import akka.actor.Terminated
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

/**
 * @author alari (name.alari@gmail.com)
 * @since 14.12.13 15:52
 */
class SessionSpec extends PlaySpecification{
  implicit val system = ActorSystem("test-sessions")

  "session actor" should {
    "timeout" in {
      val s = system.actorOf(Props(new Session(Props(new Handler.Echo), timeoutMs = 100)))
      val isTimedOut = Promise[Boolean]()
      system.actorOf(Props(new Actor{
        def receive = {
          case Terminated(a) if a == s =>
            isTimedOut.success(true)
          self ! PoisonPill
        }
        context.watch(s)
      }))

      isTimedOut.isCompleted must beFalse

      isTimedOut.future must beTrue.await(1, FiniteDuration(200, "milliseconds"))
    }
  }
}
