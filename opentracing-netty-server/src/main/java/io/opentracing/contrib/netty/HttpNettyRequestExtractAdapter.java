package io.opentracing.contrib.netty;

import io.netty.handler.codec.http.HttpRequest;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Doug Chimento
 */
public class HttpNettyRequestExtractAdapter implements TextMap {

  private List<Map.Entry<String, String>> headers;

  public HttpNettyRequestExtractAdapter(HttpRequest request) {
    this.headers = request.headers().entries();
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return this.headers.iterator();
  }

  @Override
  public void put(String key, String value) {
    throw new UnsupportedOperationException("This class should be used only with Tracer.inject()!");
  }
}
