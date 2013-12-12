package mirari.sockjs.controller

import play.api.mvc.{Action, Controller}

/**
 * @author alari
 * @since 12/11/13
 */
object TestController extends Controller {
  def index = Action {
    implicit request =>
      Ok(<html>
        <head>
          <script src="https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.min.js"></script>
          <script><![CDATA[
            window.ws = new SockJS('http://localhost:9000/echo');
            window.ws.onmessage = function(m){
              console.log(m);
              document.getElementById('out').value += m.data + '\n';
            }
            ]]>
          </script>
        </head>
        <body>
          <h1>Hi there</h1><br/>
          <a onclick="javascript:ws.send(JSON.stringify({ping:true}))">Send Message</a>
          <textarea id="out" cols="50" rows="20"></textarea>
        </body>
      </html>).withHeaders(CONTENT_TYPE -> "text/html; encoding=UTF-8")
  }
}
