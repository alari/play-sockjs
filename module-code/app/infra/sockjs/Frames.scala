package infra.sockjs

import play.api.libs.json.{JsArray, JsValue}
import org.apache.commons.lang3.StringUtils

/**
 * @author alari
 * @since 12/13/13
 */
object Frames {
  val Closed = "c"

  val Heartbeat = "h"

  val Open = "o"

  val Array = "a"

  def array(v: JsValue) = Array + "[" + JsonCodec.encodeJson(v) + "]"

  def array(vs: Seq[JsValue]) = Array + JsonCodec.encodeJson(JsArray(vs))

  def closed(code: Int, reason: String) = "c[" + code + ",\"" + reason + "\"]"

  val Closed_AnotherConnectionStillOpen = closed(2010, "Another connection still open")
  val Closed_GoAway = closed(3000, "Go away!")
  val Closed_ConnectionInterrupted = closed(1002, "Connection interrupted")

  object Format {

    import StringEscapeUtils.escapeJavaScript

    def xhr(frame: String) = frame + "\n"

    def jsonp(callback: String)(frame: String) = s"""${callback}("${escapeJavaScript(frame)}");\r\n"""

    def htmlfile(frame: String) = s"""<script>\np("${escapeJavaScript(frame)}");\n</script>\r\n"""

    def eventsource(frame: String) = s"""data: $frame\r\n\r\n"""
  }

  object Prelude {
    val xhrStreaming = StringUtils.repeat('h', 2048) + '\n'

    val eventsource = "\r\n"

    def htmlfile(callback: String) = {
     val h = s"""<!doctype html>
<html><head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head><body><h2>Don't panic!</h2>
  <script>
    document.domain = document.domain;
    var c = parent.$callback;
    c.start();
    function p(d) {c.message(d);};
    window.onload = function() {c.stop();};
  </script>""".replaceAll( """(?m)\s+$""", "")

      h + StringUtils.repeat(' ', 1024 - h.length + 14)
    }

  }

}
