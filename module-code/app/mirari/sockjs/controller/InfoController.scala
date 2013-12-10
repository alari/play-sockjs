package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}
import org.joda.time.DateTime
import mirari.sockjs.SockJS
import mirari.sockjs.SockJSService.{Info, InfoRequest}
import play.api.libs.json.Json
import mirari.sockjs.SockJS.ServiceNotFound
import concurrent.ExecutionContext.Implicits.global


/**
 * @author alari
 * @since 12/10/13
 */
object InfoController extends Controller{
  def infoOpts(service: String) = Action {
    request =>
      NoContent.withHeaders(
        ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers.get(ORIGIN).filter(_ != "null").getOrElse("*"),
        ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
        ACCESS_CONTROL_ALLOW_METHODS -> "OPTIONS, GET",
        CACHE_CONTROL -> "public; max-age=31536000",
        EXPIRES -> DateTime.now().plusYears(1).toString,
        ACCESS_CONTROL_MAX_AGE -> "1000001"
      )
  }

  def info(service: String) = Action.async {
    request =>
      SockJS.askService(service, InfoRequest).map {
        case i: Info => Ok(Json.toJson(i)).withHeaders(
          CONTENT_TYPE -> "application/json; charset=UTF-8",
          CACHE_CONTROL -> "no-store, no-cache, must-revalidate, max-age=0",
          ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers.get(ORIGIN).filter(_ != "null").getOrElse("*"),
          ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
        )
        case ServiceNotFound => NotFound
      }
  }
}
