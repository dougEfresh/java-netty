package io.opentracing.contrib.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * @author Doug Chimento
 */
public interface NettyHttpSpanDecorator {

  void onRequest(ChannelHandlerContext ctx, HttpRequest request, Span span);

  void onResponse(ChannelHandlerContext ctx, HttpResponse response, Span span);

  NettyHttpSpanDecorator STANDARD_TAGS = new NettyHttpSpanDecorator() {
    @Override
    public void onRequest(final ChannelHandlerContext ctx, final HttpRequest request, Span span) {
      Tags.COMPONENT.set(span, "netty-http-server");
      Tags.HTTP_METHOD.set(span, request.getMethod().name());
      QueryStringDecoder url = new QueryStringDecoder(request.getUri());
      Tags.HTTP_URL.set(span, url.path());
    }

    @Override
    public void onResponse(final ChannelHandlerContext ctx, final HttpResponse response, final Span span) {
      Tags.HTTP_STATUS.set(span, response.getStatus().code());
      if (response.getStatus().code() >= 500) {
        Tags.ERROR.set(span, true);
      }
    }
  };
}
