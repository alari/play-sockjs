package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}
import mirari.sockjs.SockJS
import mirari.sockjs.SockJSService.RetrieveSessionMessages
import mirari.sockjs.frame.Frame
import mirari.sockjs.SockJSSessionActor.InOut
import play.api.libs.EventSource
import concurrent.ExecutionContext.Implicits.global

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
    SockJS.askService(service, RetrieveSessionMessages(session)).map {
      case f: Frame => Ok(f)
      case InOut(in, out) =>
        Ok.chunked(out &> EventSource())
      case a =>
        play.api.Logger.error(a.toString + "????????")
        Ok
    }
  }

  def htmlfile(service: String, server: String, session: String) = Action {
    NotImplemented
  }


}
