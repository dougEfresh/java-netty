[![Build Status][ci-img]][ci] [ ![Download](https://api.bintray.com/packages/dougefresh/maven/opentracing-netty-server/images/download.svg?version=0.1.0-RC1) ](https://bintray.com/dougefresh/maven/opentracing-netty-server/0.1.0-RC1/link) [![OpenTracing Badge](https://img.shields.io/badge/OpenTracing-enabled-blue.svg)](http://opentracing.io)


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

## Trace Decorators

A default [decorator](https://github.com/dougEfresh/java-netty/blob/master/opentracing-netty-server/src/main/java/io/opentracing/contrib/netty/NettyHttpSpanDecorator.java#L19) is provider. You can add your own by implementing [NettyHttpSpanDecorator](https://github.com/dougEfresh/java-netty/blob/master/opentracing-netty-server/src/main/java/io/opentracing/contrib/netty/NettyHttpSpanDecorator.java#L13)  

## Accessing Server Span
Current server span accessible via attr .
```java
Span span = ctx.channel().attr(NettyHttpTracing.SPAN_ATTRIBUTE).get();
   
```

## Development
```shell
./gradlew clean check
```

## Special Thanks

* [java-web-servlet-filter](https://github.com/opentracing-contrib/java-web-servlet-filter)
* [brave netty instrumentation](https://github.com/openzipkin/brave)

## Release
Follow instructions in [RELEASE](RELEASE.md)


   [ci-img]: https://travis-ci.org/dougEfresh/java-netty.svg?branch=master
   [ci]: https://travis-ci.org/dougEfresh/java-netty
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-netty-server

