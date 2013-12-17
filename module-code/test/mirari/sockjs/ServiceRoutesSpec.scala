package mirari.sockjs

import play.api.test.PlaySpecification

/**
 * @author alari
 * @since 12/16/13
 */
class ServiceRoutesSpec extends PlaySpecification {
  "router" should {
    import SockJsAction._

    "dispatch greetings" in {
      SockJsService.dispatch("GET", "") must_== Greetings
      SockJsService.dispatch("GET", "/") must_== Greetings
    }
    "dispatch iframe" in {
      SockJsService.dispatch("GET", "/iframe.html") must_== Iframe
      SockJsService.dispatch("GET", "/iframe-adfgadfgdsfg.html") must_== Iframe
    }
    "dispatch info" in {
      SockJsService.dispatch("GET", "/info") must_== Info
      SockJsService.dispatch("OPTIONS", "/info") must_== InfoOptions
    }
    "dispatch raw websocket" in {
      SockJsService.dispatch("GET", "/websocket") must_== RawWebsockset
    }

    "dispatch jsonp" in {
      SockJsService.dispatch("GET", "/000/test/jsonp") must_== Jsonp("test")
    }

    "dispatch jsonp send" in {
      SockJsService.dispatch("POST", "/000/test/jsonp_send") must_== JsonpSend("test")
      SockJsService.dispatch("GET", "/000/test/jsonp_send") must_== NotFound
    }

    "dispatch xhr" in {
      SockJsService.dispatch("POST", "/000/info/xhr") must_== XhrPolling("info")
      SockJsService.dispatch("OPTIONS", "/000/info/xhr") must_== XhrPollingOptions("info")
    }

    "dispatch xhr streaming" in {
      SockJsService.dispatch("POST", "/000/info/xhr_streaming") must_== XhrStreaming("info")
      SockJsService.dispatch("OPTIONS", "/000/info/xhr_streaming") must_== XhrStreamingOptions("info")
    }

    "dispatch eventsource" in {
      SockJsService.dispatch("GET", "/000/info/eventsource") must_== EventSource("info")
    }

    "dispatch htmlfile" in {
      SockJsService.dispatch("GET", "/000/info/htmlfile") must_== HtmlFile("info")
    }

    "dispatch websocket" in {
      SockJsService.dispatch("GET", "/000/info/websocket") must_== WebSocket("info")
    }

    "dispatch all others to NotFound" in {
      SockJsService.dispatch("GET", "/000/info/htmlfil2e") must_== NotFound
      SockJsService.dispatch("POST", "/000/info/eventsource") must_== NotFound
    }
  }
}
