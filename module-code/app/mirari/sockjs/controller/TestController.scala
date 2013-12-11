package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}

/**
 * @author alari
 * @since 12/11/13
 */
object TestController extends Controller{
def index = Action {
  implicit request =>
    Ok(<html>
    <head>
      <script>
      ws = WebSocket("ws://localhost:9000/echo/000/test/websocket");
      </script>
    </head>
      <body>
      <h1>Hi there</h1>
      </body>
    </html>).withHeaders(CONTENT_TYPE -> "text/html; encoding=UTF-8")
}
}
