package com.sevenow.fulfillment.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Rejects request bodies larger than {@code sevenow.http.max-request-body-bytes} before they are parsed:
 * 413 when the declared Content-Length is too large, and a read failure (400) when a body without a
 * Content-Length (chunked) grows past the limit.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

  public static final long DEFAULT_MAX_REQUEST_BODY_BYTES = 1048576L;

  private final long maxRequestBodyBytes;

  public RequestBodySizeLimitFilter(@Value("${sevenow.http.max-request-body-bytes:" + DEFAULT_MAX_REQUEST_BODY_BYTES + "}") long maxRequestBodyBytes) {
    this.maxRequestBodyBytes = maxRequestBodyBytes;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (request.getContentLengthLong() > maxRequestBodyBytes) {
      response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Request body must be at most " + maxRequestBodyBytes + " bytes");
      return;
    }
    filterChain.doFilter(new SizeLimitedRequest(request, maxRequestBodyBytes), response);
  }

  public static class RequestBodyTooLargeException extends IOException {
    RequestBodyTooLargeException(long maxRequestBodyBytes) {
      super("Request body must be at most " + maxRequestBodyBytes + " bytes");
    }
  }

  private static class SizeLimitedRequest extends HttpServletRequestWrapper {

    private final long maxRequestBodyBytes;
    private ServletInputStream inputStream;

    SizeLimitedRequest(HttpServletRequest request, long maxRequestBodyBytes) {
      super(request);
      this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (inputStream == null)
        inputStream = new SizeLimitedInputStream(super.getInputStream(), maxRequestBodyBytes);
      return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.ISO_8859_1 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }

  private static class SizeLimitedInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxRequestBodyBytes;
    private long bytesRead;

    SizeLimitedInputStream(ServletInputStream delegate, long maxRequestBodyBytes) {
      this.delegate = delegate;
      this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b != -1)
        count(1);
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int n = delegate.read(b, off, len);
      if (n > 0)
        count(n);
      return n;
    }

    private void count(int n) throws IOException {
      bytesRead += n;
      if (bytesRead > maxRequestBodyBytes)
        throw new RequestBodyTooLargeException(maxRequestBodyBytes);
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      delegate.setReadListener(readListener);
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
}
