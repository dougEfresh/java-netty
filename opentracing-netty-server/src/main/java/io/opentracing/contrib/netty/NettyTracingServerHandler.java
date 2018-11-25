package io.opentracing.contrib.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Adds {@link Span} context to netty {@link io.netty.channel.ChannelPipeline}
 */
public class NettyTracingServerHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(NettyTracingServerHandler.class);
  private final Tracer tracer;
  private final Pattern skipPattern;
  private final List<NettyHttpSpanDecorator> decorators;

  public NettyTracingServerHandler() {
    this(GlobalTracer.get());
  }

  public NettyTracingServerHandler(Tracer tracer) {
    this(tracer, Collections.singletonList(NettyHttpSpanDecorator.STANDARD_TAGS), null);
  }

  /**
   *
   * @param tracer tracer
   * @param decorators list {@link NettyHttpSpanDecorator}
   * @param skipPattern null or pattern to exclude certain paths from tracing e.g. "/health"
   */
  public NettyTracingServerHandler(Tracer tracer, List<NettyHttpSpanDecorator> decorators, Pattern skipPattern) {
    this.tracer = tracer;
    this.skipPattern = skipPattern;
    this.decorators = decorators;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpRequest)) {
      ctx.fireChannelRead(msg);
      return;
    }
    HttpRequest request = (HttpRequest) msg;
    if (!isTraced(ctx, request)) {
      ctx.fireChannelRead(msg);
      return;
    }
    if (HttpUtil.is100ContinueExpected(request)) {
      ctx.fireChannelRead(ctx);
      return;
    }
    //Already has tracing
    if (ctx.channel().attr(NettyHttpTracing.SPAN_ATTRIBUTE).get() != null) {
      ctx.fireChannelRead(msg);
      return;
    }
    SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                                                  new HttpNettyRequestExtractAdapter(request));
    final Span span = tracer.buildSpan(request.method().name())
                              .asChildOf(extractedContext)
                              .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start();
    ctx.channel().attr(NettyHttpTracing.SPAN_ATTRIBUTE).set(span);

    for (NettyHttpSpanDecorator decorator : decorators) {
      try {
        decorator.onRequest(ctx, request, span);
      } catch (Exception e) {
        log.warn("Error with decorator onRequest {} {}",  decorator.getClass().getName(), e.getLocalizedMessage());
      }
    }

    try {
      ctx.fireChannelRead(msg);
    } catch (Exception e) {
      Tags.ERROR.set(span, Boolean.TRUE);
      span.log(logsForException(e));
      span.finish();
      throw e;
    }
  }


  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Span span = ctx.channel().attr(NettyHttpTracing.SPAN_ATTRIBUTE).get();
    if (span == null || !(msg instanceof HttpResponse)) {
      ctx.write(msg, prm);
      return;
    }

    HttpResponse response = (HttpResponse) msg;

    for (NettyHttpSpanDecorator decorator : decorators) {
      try {
        decorator.onResponse(ctx, response, span);
      } catch (Exception e) {
        log.warn("Error with decorator onResponse {} {}",  decorator.getClass().getName(), e.getLocalizedMessage());
      }
    }

    try {
      ctx.write(msg, prm);
    } finally {
      span.finish();
    }
  }

  @SuppressWarnings("unused")
  public boolean isTraced(ChannelHandlerContext ctx, HttpRequest request) {
    if (skipPattern != null) {
      return !skipPattern.matcher(request.uri()).matches();
    }
    return true;
  }

  private Map<String, String> logsForException(Throwable throwable) {
    Map<String, String> errorLog = new HashMap<>(3);
    errorLog.put("event", Tags.ERROR.getKey());

    String message = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
    if (message != null) {
      errorLog.put("message", message);
    }
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    errorLog.put("stack", sw.toString());

    return errorLog;
  }
}
