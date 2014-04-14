package infra.sockjs

import play.api.test.{WithApplication, FakeRequest, PlaySpecification}
import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import play.api.libs.iteratee.{Concurrent, Iteratee}
import scala.concurrent.Promise
import akka.pattern.ask
import infra.sockjs.transport._
import infra.sockjs.transport.Transport._


/**
 * @author alari (name.alari@gmail.com)
 * @since 13.12.13 23:52
 */
class TransportSpec extends PlaySpecification {
  implicit val system = ActorSystem("test-transport")
  implicit val request = FakeRequest()

  import Session._
  import system.dispatcher

  def genSillySession(debug: Boolean = false) = system.actorOf(Props(new Actor {
    var tr: ActorRef = null

    def receive = {
      case ct: CreateTransport =>
        tr = context.actorOf(ct.props, "transport")
        tr ! RegisterTransport
        sender ! tr

      case o: OutgoingRaw =>
        if(debug) play.api.Logger.error(s"\n$o; $tr\n")
        tr ! o

      case Incoming(m) =>
        self ! OutgoingRaw(m)

      case IncomingJson(m) =>
        self ! OutgoingRaw(m.toString())

      case "get transport" =>
        sender ! context.child("transport")
    }
  }))


  "promise transport" should {
    "push a single message" in new WithApplication {

      val session = genSillySession()

      Transport.singleFramePlex(session) should beLike[SingleFramePlex] {
        case t =>
          session ! OutgoingRaw("test")
          t.out must be("test").await
      }.await
    }
  }

  "half duplex transport" should {
    "push several messages" in new WithApplication {
      val session = genSillySession()

      Transport.halfDuplex(session) should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String]{m => msg.success(m)}
          t.out |>> iteratee

          session ! OutgoingRaw("1")

          msg.future must be("1").await

          msg = Promise[String]()

          session ! OutgoingRaw("2")

          msg.future must be("2").await

      }.await
    }

    "provide initial payload" in new WithApplication {
      val session = genSillySession()

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

    "respect maximum size of streaming" in new WithApplication {
      val session = genSillySession()

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

          (session ? "get transport")(SockJs.Timeout) must beLike[Any] {
            case Some(a) =>
              a must beAnInstanceOf[ActorRef]
          }.await


          session ! OutgoingRaw("2")
          msg.future must be("2").await

          (session ? "get transport")(SockJs.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await

      }.await
    }

    "use frame formatter with payload" in new WithApplication {
      val session = genSillySession(debug = true)

      val payload: String = "akka"

      Transport.halfDuplex(session, initialPayload = payload, maxStreamingBytes = 2, frameFormatter = a => s"($a)") should beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()

          t.out &> Concurrent.buffer(10) |>> Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }
          // payload must be sent only when out |>> is called
          msg.future must beEqualTo(payload).await
          msg = Promise[String]()

          session ! OutgoingRaw("z")

          msg.future must beEqualTo("(z)").await

          (session ? "get transport")(SockJs.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await
      }.await
    }

    "use frame formatter without payload" in new WithApplication {
      val session = genSillySession()

      Transport.halfDuplex(session, maxStreamingBytes = 2, frameFormatter = a => s"($a)") should beLike[HalfDuplex] {
        case t =>
          val msg = Promise[String]()
          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out &> Concurrent.buffer(10) |>> iteratee

          session ! OutgoingRaw("1")

          msg.future must beEqualTo("(1)").await

          (session ? "get transport")(SockJs.Timeout) must beLike[Any] {
            case None =>
              1 must_== 1
          }.await
      }.await
    }

    "act as xhr stream" in new WithApplication {
      val session = genSillySession()

      halfDuplex(session, Frames.Prelude.xhrStreaming, Frames.Format.xhr, 3096) must beLike[HalfDuplex] {
        case t =>
          var msg = Promise[String]()
          val iteratee = Iteratee.foreach[String] {
            m =>
              msg.success(m)
          }

          t.out |>> iteratee

          msg.future must beEqualTo(Frames.Prelude.xhrStreaming).await
          msg = Promise[String]()


          session ! OutgoingRaw("1")

          msg.future must beEqualTo("1\n").await
          msg = Promise[String]()

          (session ? "get transport")(SockJs.Timeout) must beLike[Any] {
            case Some(a) =>
              a must beAnInstanceOf[ActorRef]
          }.await


          session ! OutgoingRaw("2")
          msg.future must beEqualTo("2\n").await
      }.await
    }
  }

  "full duplex transport" should {
    "push messages like half duplex" in new WithApplication {
      val session = genSillySession()

      Transport.fullDuplex(session) should beLike[FullDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String]{m => msg.success(m)}
          t.out |>> iteratee

          session ! OutgoingRaw("1")

          msg.future must be("1").await

          msg = Promise[String]()

          session ! OutgoingRaw("2")

          msg.future must be("2").await

      }.await
    }

    "push and consume messages" in new WithApplication {
      val session = genSillySession()

      Transport.fullDuplex(session) should beLike[FullDuplex] {
        case t =>
          var msg = Promise[String]()

          val iteratee = Iteratee.foreach[String](msg.success(_))
          t.out |>> iteratee

          val (out, channel) = Concurrent.broadcast[String]

          out |>> t.in

          channel.push("\"0\"")

          msg.future must beEqualTo("\"0\"").await

          msg = Promise[String]()

          session ! OutgoingRaw("\"1\"")

          msg.future must beEqualTo("\"1\"").await

          msg = Promise[String]()

          channel.push("\"2\"")

          msg.future must beEqualTo("\"2\"").await

      }.await
    }
  }
}
