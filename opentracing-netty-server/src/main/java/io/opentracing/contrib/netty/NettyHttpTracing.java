package io.opentracing.contrib.netty;

import io.netty.util.AttributeKey;
import io.opentracing.Span;

public final class NettyHttpTracing {
  public static final AttributeKey<Span> SPAN_ATTRIBUTE = AttributeKey.valueOf(Span.class.getName());
}
