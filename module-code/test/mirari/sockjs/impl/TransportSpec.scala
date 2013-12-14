package mirari.sockjs.impl

import play.api.test.{FakeRequest, PlaySpecification}
import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import play.api.libs.iteratee.{Concurrent, Iteratee}
import scala.concurrent.Promise
import akka.pattern.ask


/**
 * @author alari (name.alari@gmail.com)
 * @since 13.12.13 23:52
 */
class TransportSpec extends PlaySpecification {
  implicit val system = ActorSystem("test-transport")
  implicit val request = FakeRequest()

  import Session._
  import Transport._
  import system.dispatcher

  def genSillySession = system.actorOf(Props(new Actor {
    var tr: ActorRef = null

    def receive = {
      case CreateTransport(t, _) =>
        tr = context.actorOf(t, "transport")
        tr ! RegisterTransport
        sender ! tr

      case o@OutgoingRaw(m) =>
        tr ! o

      case Incoming(m) =>
        self ! OutgoingRaw(m)

      case "get transport" =>
        sender ! context.child("transport")
    }
  }))


  "promise transport" should {
    "push a single message" in {

      val session = genSillySession

      Transport.singleFramePlex(session) should beLike[SingleFramePlex] {
        case t =>
          session ! OutgoingRaw("test")
          t.out must be("test").await
      }.await
    }
  }

  "half duplex transport" should {
    "push several messages" in {
      val session = genSillySession

      Transport.halfDuplex(session) should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String](msg.success(_))
          t.out |>> iteratee

          session ! OutgoingRaw("1")

          msg.future must be("1").await

          msg = Promise[String]()

          session ! OutgoingRaw("2")

          msg.future must be("2").await

      }.await
    }

    "provide initial payload" in {
      val session = genSillySession

      Transport.halfDuplex(session, "z") should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()
          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out |>> iteratee

          msg.future must be("z").await
          msg = Promise[String]()

          session ! OutgoingRaw("1")

          msg.future must be("1").await
      }.await
    }

    "respect maximum size of streaming" in {
      val session = genSillySession

      Transport.halfDuplex(session, "z", maxStreamingBytes = 2) should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()
          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out |>> iteratee

          msg.future must be("z").await
          msg = Promise[String]()


          session ! OutgoingRaw("1")

          msg.future must be("1").await
          msg = Promise[String]()

          (session ? "get transport")(Transport.Timeout) must beLike[Any] {
            case Some(a) =>
              a must beAnInstanceOf[ActorRef]
          }.await


          session ! OutgoingRaw("2")
          msg.future must be("2").await

          (session ? "get transport")(Transport.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await

      }.await
    }

    "use frame formatter with payload" in {
      val session = genSillySession

      val payload: String = "akka"

      Transport.halfDuplex(session, initialPayload = payload, maxStreamingBytes = 2, frameFormatter = a => s"($a)") should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out |>> iteratee
          // payload must be sent only when "out" is called
          // or even out |>>
          if (payload != null) {
            msg.future must be(payload).await
          }
          msg = Promise[String]()

          session ! OutgoingRaw("1")

          msg.future must beEqualTo("(1)").await

          (session ? "get transport")(Transport.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await
      }.await
    }

    "use frame formatter without payload" in {
      val session = genSillySession

      val payload: String = null

      Transport.halfDuplex(session, initialPayload = payload, maxStreamingBytes = 2, frameFormatter = a => s"($a)") should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()
          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out |>> iteratee

          if (payload != null) {
            msg.future must be(payload).await
          }
          msg = Promise[String]()

          session ! OutgoingRaw("1")

          msg.future must beEqualTo("(1)").await

          (session ? "get transport")(Transport.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await
      }.await
    }
  }

  "full duplex transport" should {
    "push messages like half duplex" in {
      val session = genSillySession

      Transport.fullDuplex(session) should beLike[FullDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String](msg.success(_))
          t.out |>> iteratee

          session ! OutgoingRaw("1")

          msg.future must be("1").await

          msg = Promise[String]()

          session ! OutgoingRaw("2")

          msg.future must be("2").await

      }.await
    }

    "push and consume messages" in {
      val session = genSillySession

      Transport.fullDuplex(session) should beLike[FullDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String](msg.success(_))
          t.out |>> iteratee

          val (out, channel) = Concurrent.broadcast[String]

          out |>> t.in

          channel.push("0")

          msg.future must be("0").await

          msg = Promise[String]()

          session ! OutgoingRaw("1")

          msg.future must be("1").await

          msg = Promise[String]()

          channel.push("2")

          msg.future must be("2").await

      }.await
    }
  }
}
