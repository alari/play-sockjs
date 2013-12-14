package mirari.sockjs.impl

import play.api.test.{FakeRequest, PlaySpecification}
import akka.actor._
import akka.actor.Terminated
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration
import play.api.libs.json.JsString

/**
 * @author alari (name.alari@gmail.com)
 * @since 14.12.13 15:52
 */
class SessionSpec extends PlaySpecification {
  implicit val system = ActorSystem("test-sessions")
  implicit val request = FakeRequest()

  import Transport._

  def echoSession = system.actorOf(Props(new Session(Props(new Handler.Echo), timeoutMs = 100)))

  "session actor" should {
    "timeout" in {
      val s = system.actorOf(Props(new Session(Props(new Handler.Echo), timeoutMs = 100)))
      val isTimedOut = Promise[Boolean]()
      system.actorOf(Props(new Actor {
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

    "register transport on a new session" in {
      val s = echoSession
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must beEqualTo(Frames.Open).await
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              incomingFrame("\"x\"", s)
              tt.out must beEqualTo(Frames.array(JsString("x"))).await
          }.await
      }.await
    }

    "reject second transport on a new session" in {
      val s = echoSession
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must be(Frames.Open).await
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              tt.out.isCompleted must beFalse
              singleFramePlex(s) must beLike[SingleFramePlex] {
                case ttt =>
                  ttt.out must beEqualTo(Frames.Closed_AnotherConnectionStillOpen).await
                  tt.out.isCompleted must beFalse
                  incomingFrame("\"x\"", s)
                  tt.out must beEqualTo(Frames.array(JsString("x"))).await
              }.await
          }.await
      }.await
    }

    "close the session and send closed messages to new transports" in {
      val s = echoSession
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must be(Frames.Open).await
          s ! Session.Close
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              tt.out must beEqualTo(Frames.Closed_GoAway).await
              singleFramePlex(s) must beLike[SingleFramePlex] {
                case ttt =>
                  ttt.out must beEqualTo(Frames.Closed_GoAway).await
              }.await
          }.await
      }.await
    }
    //    "connect second transport after the first one was used"
    //    "send heartbeat frames"
    //    "handle pending messages correctly, send them in a single frame"
  }
}
