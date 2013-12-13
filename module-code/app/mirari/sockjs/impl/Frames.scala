package mirari.sockjs.impl

/**
 * @author alari
 * @since 12/13/13
 */
object Frames {
  val Closed = "c"

  val Heartbeat = "h"

  val Open = "o"

  def closed(code: Int, reason: String) = "c[" + code + ",\"" + reason + "\"]"
}