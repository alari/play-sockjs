package mirari.sockjs.transport

import play.api.mvc.Action

/**
 * @author alari
 * @since 12/11/13
 */
class JsonpTransport {

}

object JsonpController extends TransportController {
  def jsonp(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def jsonp_send(service: String, server: String, session: String) = Action {
    NotImplemented
  }
}