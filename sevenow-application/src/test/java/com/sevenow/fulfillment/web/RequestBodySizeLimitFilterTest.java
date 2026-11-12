package com.sevenow.fulfillment.web;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class RequestBodySizeLimitFilterTest {

  private static final long LIMIT = 16;

  private final RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(LIMIT);

  @Test
  public void shouldRejectDeclaredContentLengthOverLimitWithoutCallingChain() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.setContent(new byte[(int) LIMIT + 1]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(413, response.getStatus());
    assertNull(chain.getRequest());
  }

  @Test
  public void shouldPassBodyWithinLimit() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
    request.setContent("{\"a\":1}".getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertEquals("{\"a\":1}", StreamUtils.copyToString(chain.getRequest().getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
  }

  @Test
  public void shouldFailReadingBodyWithoutContentLengthOnceOverLimit() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders") {
      @Override
      public long getContentLengthLong() {
        return -1;
      }

      @Override
      public int getContentLength() {
        return -1;
      }
    };
    request.setContent(new byte[(int) LIMIT * 4]);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    HttpServletRequest wrapped = (HttpServletRequest) chain.getRequest();
    try {
      StreamUtils.copyToByteArray(wrapped.getInputStream());
      fail("expected RequestBodyTooLargeException");
    } catch (RequestBodySizeLimitFilter.RequestBodyTooLargeException e) {
      // expected
    }
  }
}
