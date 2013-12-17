package mirari.sockjs

import play.api.test.{FakeRequest, PlaySpecification}
import akka.actor._
import akka.actor.Terminated
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration
import play.api.libs.json.{JsNumber, JsString}

/**
 * @author alari (name.alari@gmail.com)
 * @since 14.12.13 15:52
 */
class SessionSpec extends PlaySpecification {
  implicit val system = ActorSystem("test-sessions")
  implicit val request = FakeRequest()

  import transport.Transport._

  def echoSession() = system.actorOf(Props(new Session(Props(new SockJsHandler.Echo), timeoutMs = 100, heartbeatPeriodMs = 250)))

  "session actor" should {
    "timeout" in {
      val s = echoSession()
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
      val s = echoSession()
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


    "close the session and send closed messages to new transports" in {
      val s = echoSession()
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

    "reject second transport on a new session" in {
      val s = echoSession()
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

    "connect second transport after the first one was used" in {
      val s = echoSession()
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must be(Frames.Open).await
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              incomingFrame("1", s)
              tt.out must beEqualTo(Frames.array(JsNumber(1))).await

              incomingFrame("2", s)
              incomingFrame("3", s)
              incomingFrame("4", s)

              singleFramePlex(s) must beLike[SingleFramePlex] {
                case ttt =>
                  ttt.out must beEqualTo(Frames.array(Seq(JsNumber(2), JsNumber(3), JsNumber(4)))).await
              }.await
          }.await
      }.await
    }

    "send heartbeat frames" in {
      val s = echoSession()
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must be(Frames.Open).await
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              tt.out.isCompleted must beFalse
              tt.out must beEqualTo(Frames.Heartbeat).await(1, FiniteDuration(300, "milliseconds"))
          }.await
      }.await
    }

    "handle pending messages correctly, send them in a single frame" in {
      val s = echoSession()
      incomingFrame("1", s)
      incomingFrame("2", s)
      incomingFrame("3", s)
      singleFramePlex(s) must beLike[SingleFramePlex] {
        case t =>
          t.out must be(Frames.Open).await
          incomingFrame("4", s)
          singleFramePlex(s) must beLike[SingleFramePlex] {
            case tt =>
              tt.out must beEqualTo(Frames.array(Seq(1, 2, 3, 4).map(JsNumber.apply(_)))).await
          }.await
      }.await
    }
  }
}
