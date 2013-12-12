package mirari.sockjs.frames

import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils

class MessageFrame(sockJsMessages: Array[Byte]) {
  
  def get(appendNewline: Boolean) = {
    val message: ArrayBuffer[Byte] = new ArrayBuffer[Byte]()
    message.append('a')
    message.appendAll(sockJsMessages)
    if (appendNewline) message.append('\n')
    message
  }
  
}

object SockJsFrames {
  val OPEN_FRAME         = "o"
  val OPEN_FRAME_NL      = "o\n"
  val HEARTBEAT_FRAME    = "h"
  val HEARTBEAT_FRAME_NL = "h\n"
  val XHR_STREAM_H_BLOCK = StringUtils.repeat("h", 2048).toCharArray.map(_.toByte) :+ '\n'.toByte
  def closingFrame(code: Int, reason: String) = s"""c[$code,"$reason"]"""

  def messageFrame(sockJsMessages: Array[Byte], appendNewline: Boolean) = new MessageFrame(sockJsMessages).get(appendNewline)
}