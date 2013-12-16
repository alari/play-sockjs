package mirari.sockjs.impl

import play.api.test.PlaySpecification

/**
 * @author alari
 * @since 12/16/13
 */
class ServiceRoutesSpec extends PlaySpecification{
  "router" should {
    "dispatch greetings" in {
      ServiceRoutes.dispatch("GET", "") must_== ServiceRoutes.Greetings
      ServiceRoutes.dispatch("GET", "/") must_== ServiceRoutes.Greetings
    }
    "dispatch iframe" in {
      ServiceRoutes.dispatch("GET", "/iframe.html") must_== ServiceRoutes.Iframe
      ServiceRoutes.dispatch("GET", "/iframe-adfgadfgdsfg.html") must_== ServiceRoutes.Iframe
    }
    "dispatch info" in {
      ServiceRoutes.dispatch("GET", "/info") must_== ServiceRoutes.Info
      ServiceRoutes.dispatch("OPTIONS", "/info") must_== ServiceRoutes.InfoOptions
    }
    "dispatch raw websocket" in {
      ServiceRoutes.dispatch("GET", "/websocket") must_== ServiceRoutes.RawWebsockset
    }

    "dispatch jsonp" in {
      ServiceRoutes.dispatch("GET", "/000/test/jsonp") must_== ServiceRoutes.Jsonp("test")
    }

    "dispatch jsonp send" in {
      ServiceRoutes.dispatch("POST", "/000/test/jsonp_send") must_== ServiceRoutes.JsonpSend("test")
      ServiceRoutes.dispatch("GET", "/000/test/jsonp_send") must_== ServiceRoutes.NotFound
    }

    "dispatch xhr" in {
      ServiceRoutes.dispatch("POST", "/000/info/xhr") must_== ServiceRoutes.XhrPolling("info")
      ServiceRoutes.dispatch("OPTIONS", "/000/info/xhr") must_== ServiceRoutes.XhrPollingOptions("info")
    }

    "dispatch xhr streaming" in {
      ServiceRoutes.dispatch("POST", "/000/info/xhr_streaming") must_== ServiceRoutes.XhrStreaming("info")
      ServiceRoutes.dispatch("OPTIONS", "/000/info/xhr_streaming") must_== ServiceRoutes.XhrStreamingOptions("info")
    }

    "dispatch eventsource" in {
      ServiceRoutes.dispatch("GET", "/000/info/eventsource") must_== ServiceRoutes.EventSource("info")
    }

    "dispatch htmlfile" in {
      ServiceRoutes.dispatch("GET", "/000/info/htmlfile") must_== ServiceRoutes.HtmlFile("info")
    }

    "dispatch websocket" in {
      ServiceRoutes.dispatch("GET", "/000/info/websocket") must_== ServiceRoutes.WebSocket("info")
    }

    "dispatch all others to NotFound" in {
      ServiceRoutes.dispatch("GET", "/000/info/htmlfil2e") must_== ServiceRoutes.NotFound
      ServiceRoutes.dispatch("POST", "/000/info/eventsource") must_== ServiceRoutes.NotFound
    }
  }
}
