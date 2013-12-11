package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author alari
 * @since 12/10/13
 */
object ChunkingController extends Controller{
  def chunking_testOpts(service: String) = Action {
    NotImplemented
  }

  def chunking_test(service: String) = Action {
    NotImplemented
  }

  def eventsource(service: String, server: String, session: String) = Action.async {
    Future(NotImplemented)
  }

  def htmlfile(service: String, server: String, session: String) = Action {
    NotImplemented
  }


}
