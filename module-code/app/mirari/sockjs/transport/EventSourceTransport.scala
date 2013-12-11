package mirari.sockjs.transport

import play.api.mvc.Action
import scala.concurrent.Future

/**
 * @author alari
 * @since 12/11/13
 */
class EventSourceTransport {

}

object EventSourceController extends TransportController {
  def eventsource(service: String, server: String, session: String) = Action.async {
    Future(NotImplemented)
  }
}