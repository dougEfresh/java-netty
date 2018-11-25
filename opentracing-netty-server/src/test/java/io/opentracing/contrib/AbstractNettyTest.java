
package io.opentracing.contrib;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentracing.contrib.netty.NettyHttpSpanDecorator;
import io.opentracing.contrib.netty.NettyTracingServerHandler;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;


/**
 */
public abstract class AbstractNettyTest {
  protected OkHttpClient client = new OkHttpClient();
  EventLoopGroup parentGroup;
  EventLoopGroup childGroup;
  int port;
  protected MockTracer mockTracer;

  abstract List<NettyHttpSpanDecorator> decorators();

  @Before
  public void beforeTest() throws Exception {
    mockTracer = Mockito.spy(new MockTracer(new ThreadLocalScopeManager(), MockTracer.Propagator.TEXT_MAP));
    stop();
    parentGroup = new NioEventLoopGroup(1);
    childGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(final Channel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new NettyTracingServerHandler(mockTracer,
                                                decorators(),
                                                Pattern.compile("/health")));
        p.addLast(new Handler());
      }
    };
    b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class).childHandler(initializer);

    Channel ch = b.bind(0).sync().channel();
    port = ((InetSocketAddress) ch.localAddress()).getPort();
  }

  protected String url(String path) {
    return "http://127.0.0.1:" + port + path;
  }

  @After
  public void stop() {
    if (parentGroup != null) {
      parentGroup.shutdownGracefully();
    }
    if (childGroup != null) {
      childGroup.shutdownGracefully();
    }
    mockTracer.reset();
  }

  Callable<Integer> reportedSpansSize() {
    return new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return mockTracer.finishedSpans().size();
      }
    };
  }
}
