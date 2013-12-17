package mirari.sockjs

import play.api.test.PlaySpecification
import play.api.libs.json.JsString

/**
 * @author alari (name.alari@gmail.com)
 * @since 14.12.13 14:57
 */
class FramesSpec extends PlaySpecification {
  "common frames builder" should {
    "produce correct a-frames" in {
      Frames.array(JsString("a")) must_== "a[\"a\"]"
      Frames.array(Seq(JsString("a"), JsString("b"))) must_== "a[\"a\",\"b\"]"
    }
  }

  "frames formatter" should {
    "format xhr" in {
      Frames.Format.xhr(Frames.Open) must_== "o\n"
    }

    "format jsonp" in {
      Frames.Format.jsonp("callback")(Frames.array(JsString("x"))) must_== "callback(\"a[\\\"x\\\"]\");\r\n"
    }

    "format html" in {
      Frames.Format.htmlfile(Frames.Open) must_== "<script>\np(\"o\");\n</script>\r\n"
    }

    "format eventsource" in {
      Frames.Format.eventsource(Frames.Open) must_== "data: o\r\n\r\n"
      Frames.Format.eventsource(Frames.array(JsString("  \u0000\n\r "))) must_== "data: a[\"  \\u0000\\n\\r \"]\r\n\r\n"
    }
  }
}
