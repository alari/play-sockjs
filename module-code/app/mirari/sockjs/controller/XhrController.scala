package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global


/**
 * @author alari
 * @since 12/10/13
 */
object XhrController extends Controller{
  def xhr(service: String, server: String, session: String) = Action.async {
    Future(NotImplemented)
  }

  def xhrOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_send(service: String, server: String, session: String) = Action.async(parse.anyContent) {
    request =>
      Future(NotImplemented)
  }

  def xhr_sendOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streaming(service: String, server: String, session: String) = Action {
    NotImplemented
  }

  def xhr_streamingOpts(service: String, server: String, session: String) = Action {
    NotImplemented
  }
}
