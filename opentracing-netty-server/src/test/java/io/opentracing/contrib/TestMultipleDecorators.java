package io.opentracing.contrib;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Span;
import io.opentracing.contrib.netty.NettyHttpSpanDecorator;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
import java.util.Arrays;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

/**
 */
public class TestMultipleDecorators extends AbstractNettyTest {

  @Test
  public void testDecorators() throws Exception {
    Request request = new Request.Builder().url(url("/foo")).build();
    try (Response response = client.newCall(request).execute()) {
      assertTrue(response.isSuccessful());
    }
    Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    MockSpan mockSpan = mockSpans.get(0);
    assertNotNull(mockSpan.operationName());
    assertEquals("customOperationName", mockSpan.operationName());
    assertNotNull(mockSpan.tags());
    assertFalse(mockSpan.tags().isEmpty());
    assertEquals("/foo", mockSpan.tags().get(Tags.HTTP_URL.getKey()));
    assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    assertEquals("netty-http-server", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals("server", mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
    assertEquals("SERVICE", mockSpan.tags().get(Tags.SERVICE.getKey()));
    assertEquals("200", mockSpan.tags().get(Tags.DB_USER.getKey()));
  }

  @Override
  List<NettyHttpSpanDecorator> decorators() {
    return Arrays.asList(NettyHttpSpanDecorator.STANDARD_TAGS, new ErrorDecorator(), new OperationName());
  }

  class ErrorDecorator implements NettyHttpSpanDecorator {
    @Override
    public void onRequest(ChannelHandlerContext ctx, HttpRequest request, Span span) {
      throw new RuntimeException("oh no!");
    }

    @Override
    public void onResponse(ChannelHandlerContext ctx, HttpResponse response, Span span) {
      Tags.SERVICE.set(span, "SERVICE");
      throw new IllegalArgumentException("I failed");
    }
  }

  class OperationName implements NettyHttpSpanDecorator {
    @Override
    public void onRequest(ChannelHandlerContext ctx, HttpRequest request, Span span) {
      span.setOperationName("customOperationName");
    }

    @Override
    public void onResponse(ChannelHandlerContext ctx, HttpResponse response, Span span) {
      Tags.DB_USER.set(span, response.status().codeAsText().toString());
    }
  }
}
