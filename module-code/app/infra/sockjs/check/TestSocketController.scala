package infra.sockjs.check

import play.api.mvc.{Controller, Action}

/**
 * @author alari
 * @since 6/24/14
 */
object TestSocketController extends Controller{

  def index = Action {
    implicit request =>
      Ok(infra.sockjs.check.html.index())
  }
}
