play-sockjs
===========

SockJS protocol implementation on Play Framework. Makes heavy use of *Iteratees/Enumerators* and *akka*: every connection, actually being a state, is backed by an _Actor_.

Quick Intro
===========

1. Build and install `play-sockjs`
-----------

You can use my Artifactory to play with a module: 

```scala
resolvers += "quonb" at "http://mvn.quonb.org/repo/"

libraryDependencies += "ru.mirari" %% "play-sockjs" % "1.0-SNAPSHOT"
```

It is not guaranteed to work, so you'd better to build `play-sockjs` locally or publish to your private repository.

2. Create a SockJS Service
------------

You can create several different SockJS services by simply subclassing `mirari.sockjs.SockJsService`:

```scala
package example

import mirari.sockjs.SockJsService

object ServiceExample extends SockJsService
```

3. Configure routes
-------------

```
->   /sockjs  example.ServiceExample
```

Yes, that simple.

4. Connect with SockJS
--------------

When you specify the service route in SockJS client, well, you'll get an echo on all transports.

Customizing the logic
==============

Sure, echo is not of business value. You should provide actor props for socket handler to add your logic. Other service settings are self-explanatory.

```scala
object ServiceExample extends SockJsService {
  override val websocketEnabled : scala.Boolean = { /* compiled code */ }
  override val cookieNeeded : scala.Boolean = { /* compiled code */ }
  override val sessionTimeoutMs : scala.Int = { /* compiled code */ }
  override val heartbeatPeriodMs : scala.Int = { /* compiled code */ }
  override val maxBytesSent : scala.Int = { /* compiled code */ }
  override val maxBytesReceived : scala.Int = { /* compiled code */ }
  override val clientScriptSrc
  
  
  override val handlerProps : akka.actor.Props = akka.actor.Props[ExampleHandler]
}

// Handler represents a SockJS Session
class ExampleHandler extends Actor with SockJsHandler {
  def receive = {
    case SockJsHandler.Request(request) =>
      // it's a RequestHeader of every transport connection
      // you may use it to authenticate user or whatever
  
    case SockJsHandler.Incoming(msg) =>
      // msg is JsValue, so you are able to parse and handle it
      // We'll just echo it back
      self ! SockJsHandler.Outgoing(msg)
  
    // message to send to a socket
    case SockJsHandler.Outgoing(msg) =>
      // this method actually performs sending
      send(msg)
  }
}
```

That's all. You have an actor. It is created for every session and destroyed on session timeout. You can change SockJS service properties by overriding `val`s in your service class. 

Probably you will need to somehow manage your actors, for example, handle user broadcasts, deliver resource updates only to those who are interested in, and so on. It's not the part of SockJS protocol, so I extracted this logic into separate module: [play-hubs](https://github.com/alari/play-hubs). It offers pub-sub support, persisting user state in actors, and socket message routing helpers.

Further development
===========

Module is under development and is used by live project. However, there are bugs. It doesn't yet pass all the tests of SockJS protocol: some of tips and trics for extremely old software are not yet implemented.

So contributions are welcome!

Sponsor
===========

Development of this module is generally sponsored by Gipsetter.
