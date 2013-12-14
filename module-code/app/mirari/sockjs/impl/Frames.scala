package mirari.sockjs.impl

import play.api.libs.json.{JsArray, JsValue}

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

  object Format {
    import StringEscapeUtils.escapeJavaScript

    def xhr(frame: String) = frame + "\n"

    def jsonp(callback: String)(frame: String) = s"""${callback}("${escapeJavaScript(frame)}");\r\n"""

    def htmlfile(frame: String) = s"""<script>\np("${escapeJavaScript(frame)}");\n</script>\r\n"""

    def eventsource(frame: String) = s"""data: $frame\r\n\r\n"""
  }
}