package mirari.sockjs.channels

import mirari.sockjs.{SockJs, SockJsHandler}
import akka.actor.{ActorRef, Props}

/**
 * @author alari
 * @since 12/17/13
 */
trait PubSubHandler extends SockJsHandler{
  def pushToChannel(message: Any, path: String*) =
    SockJs.channels ! ChannelsBranch.PushToChannel(message, path.toList)

  def tellToChannel(props: Props, path: String*)(message: Any) =
    SockJs.channels ! ChannelsBranch.TellToChannel(props, message, path.toList)

  def subscribe(props: Props, state: Any, path: String*) =
  SockJs.channels ! ChannelsBranch.TellToChannel(props, ChannelLeaf.Subscribe(state), path.toList)

  def unsubscribe(path: String*) =
    SockJs.channels ! ChannelsBranch.PushToChannel(ChannelLeaf.Unsubscribe, path.toList)

  def handleByChannel(props: Props, state: Any, path: String*)(message: Any) =
    SockJs.channels ! ChannelsBranch.TellToChannel(props, ChannelLeaf.Handle(state, message), path.toList)
}
