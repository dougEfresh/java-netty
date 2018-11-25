package io.opentracing.contrib;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import io.opentracing.contrib.netty.NettyHttpSpanDecorator;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
import java.util.Collections;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

public class TestStandardDecorator extends AbstractNettyTest {


  @Override
  List<NettyHttpSpanDecorator> decorators() {
    return Collections.singletonList(NettyHttpSpanDecorator.STANDARD_TAGS);
  }

  @Test
  public void verifyNoSpan() throws Exception {
    Request request = new Request.Builder().url(url("/health")).build();
    try (Response response = client.newCall(request).execute()) {
      assertTrue(response.isSuccessful());
    }
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertTrue(mockSpans.isEmpty());
  }


  @Test
  public void verifySpan() throws Exception {
    Request request = new Request.Builder().url(url("/foo")).build();
    try (Response response = client.newCall(request).execute()) {
      assertTrue(response.isSuccessful());
    }
    Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertFalse(mockSpans.isEmpty());
    MockSpan mockSpan = mockSpans.get(0);
    assertNotNull(mockSpan.operationName());
    assertEquals("GET", mockSpan.operationName());
    assertNotNull(mockSpan.tags());
    assertFalse(mockSpan.tags().isEmpty());
    assertEquals("/foo", mockSpan.tags().get(Tags.HTTP_URL.getKey()));
    assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    assertEquals("netty-http-server", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals("server", mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
  }


  @Test
  public void verifyException() throws Exception {
    Request request = new Request.Builder().url(url("/exception")).build();
    try (Response response = client.newCall(request).execute()) {
      assertFalse(response.isSuccessful());
    }
    Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertFalse(mockSpans.isEmpty());
    MockSpan mockSpan = mockSpans.get(0);
    assertNotNull(mockSpan.operationName());
    assertEquals("GET", mockSpan.operationName());
    assertNotNull(mockSpan.tags());
    assertFalse(mockSpan.tags().isEmpty());
    assertEquals("/exception", mockSpan.tags().get(Tags.HTTP_URL.getKey()));
    assertEquals(500, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    assertEquals("netty-http-server", mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals("server", mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
    assertEquals(true, mockSpan.tags().get(Tags.ERROR.getKey()));
  }
}
