[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Java Netty Server Instrumentation

This library provides instrumentation for Java Netty Server applications.

## Initialization

```java
ServerBootstrap b = new ServerBootstrap();
b.option(ChannelOption.SO_BACKLOG, 1024);
ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
@Override
 protected void initChannel(final Channel ch) throws Exception {
  ChannelPipeline p = ch.pipeline();
  p.addLast(new HttpServerCodec());
  p.addLast(new NettyTracingServerHandler(tracer,
                                          decorators(),
                                          Pattern.compile("/health")));
  p.addLast(new Handler());
    }
  };
b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class).childHandler(initializer);

Channel ch = b.bind(0).sync().channel();
port = ((InetSocketAddress) ch.localAddress()).getPort();
```

## Tracer Decorators

TBD

## Accessing Server Span
Current server span accessible via attr .
```java
Span span = ctx.channel().attr(NettyHttpTracing.SPAN_ATTRIBUTE).get();
   
```

## Development
```shell
./gradlew clean check
```

## Release
Follow instructions in [RELEASE](RELEASE.md)


   [ci-img]: https://travis-ci.org/dougEfresh/java-netty.svg?branch=master
   [ci]: https://travis-ci.org/dougEfresh/java-netty
   [maven-img]: https://img.shields.io/maven-central/v/com.github.dougefresh.opentracing/opentracing-netty-server.svg?maxAge=2592000
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-netty-server
