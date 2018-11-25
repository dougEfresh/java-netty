package io.opentracing.contrib;

import static io.opentracing.contrib.netty.NettyHttpSpanDecorator.STANDARD_TAGS;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.opentracing.contrib.netty.NettyTracingServerHandler;
import io.opentracing.noop.NoopTracerFactory;
import java.util.regex.Pattern;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 */
public class NettyTracingServerHandlerTest {

  @Test
  public void isTraced() {
    NettyTracingServerHandler handler = new NettyTracingServerHandler(NoopTracerFactory.create(),
                                                                      singletonList(STANDARD_TAGS),
                                                                      Pattern.compile("/health"));
    HttpRequest request = Mockito.mock(HttpRequest.class);
    when(request.uri()).thenReturn("/health");
    assertFalse(handler.isTraced(Mockito.mock(ChannelHandlerContext.class), request));

    reset(request);
    when(request.uri()).thenReturn("/api/users");
    assertTrue(handler.isTraced(Mockito.mock(ChannelHandlerContext.class), request));
  }
}
