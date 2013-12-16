package mirari.sockjs.impl

import play.api.test.PlaySpecification

/**
 * @author alari
 * @since 12/16/13
 */
class ServiceRoutesSpec extends PlaySpecification{
  "router" should {
    "dispatch greetings" in {
      SockJsService.dispatch("GET", "") must_== SockJsService.Greetings
      SockJsService.dispatch("GET", "/") must_== SockJsService.Greetings
    }
    "dispatch iframe" in {
      SockJsService.dispatch("GET", "/iframe.html") must_== SockJsService.Iframe
      SockJsService.dispatch("GET", "/iframe-adfgadfgdsfg.html") must_== SockJsService.Iframe
    }
    "dispatch info" in {
      SockJsService.dispatch("GET", "/info") must_== SockJsService.Info
      SockJsService.dispatch("OPTIONS", "/info") must_== SockJsService.InfoOptions
    }
    "dispatch raw websocket" in {
      SockJsService.dispatch("GET", "/websocket") must_== SockJsService.RawWebsockset
    }

    "dispatch jsonp" in {
      SockJsService.dispatch("GET", "/000/test/jsonp") must_== SockJsService.Jsonp("test")
    }

    "dispatch jsonp send" in {
      SockJsService.dispatch("POST", "/000/test/jsonp_send") must_== SockJsService.JsonpSend("test")
      SockJsService.dispatch("GET", "/000/test/jsonp_send") must_== SockJsService.NotFound
    }

    "dispatch xhr" in {
      SockJsService.dispatch("POST", "/000/info/xhr") must_== SockJsService.XhrPolling("info")
      SockJsService.dispatch("OPTIONS", "/000/info/xhr") must_== SockJsService.XhrPollingOptions("info")
    }

    "dispatch xhr streaming" in {
      SockJsService.dispatch("POST", "/000/info/xhr_streaming") must_== SockJsService.XhrStreaming("info")
      SockJsService.dispatch("OPTIONS", "/000/info/xhr_streaming") must_== SockJsService.XhrStreamingOptions("info")
    }

    "dispatch eventsource" in {
      SockJsService.dispatch("GET", "/000/info/eventsource") must_== SockJsService.EventSource("info")
    }

    "dispatch htmlfile" in {
      SockJsService.dispatch("GET", "/000/info/htmlfile") must_== SockJsService.HtmlFile("info")
    }

    "dispatch websocket" in {
      SockJsService.dispatch("GET", "/000/info/websocket") must_== SockJsService.WebSocket("info")
    }

    "dispatch all others to NotFound" in {
      SockJsService.dispatch("GET", "/000/info/htmlfil2e") must_== SockJsService.NotFound
      SockJsService.dispatch("POST", "/000/info/eventsource") must_== SockJsService.NotFound
    }
  }
}
