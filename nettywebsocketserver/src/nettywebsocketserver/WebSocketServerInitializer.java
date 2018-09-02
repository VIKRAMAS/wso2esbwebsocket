package nettywebsocketserver;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

public class WebSocketServerInitializer
  extends ChannelInitializer<SocketChannel>
{
  private final SslContext sslCtx;
  private String subscriberPath;
  
  public WebSocketServerInitializer(SslContext sslCtx)
  {
    this.sslCtx = sslCtx;
  }
  
  public void setWebsockeSubscriberPath(String subscriberPath) {
    this.subscriberPath = subscriberPath;
  }
  
  public void initChannel(SocketChannel ch) throws Exception
  {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslCtx != null) {
      pipeline.addLast(new ChannelHandler[] { sslCtx.newHandler(ch.alloc()) });
    }
    pipeline.addLast(new ChannelHandler[] { new HttpServerCodec() });
    pipeline.addLast(new ChannelHandler[] { new HttpObjectAggregator(65536) });
    WebSocketServerHandler handler = new WebSocketServerHandler();
    if (subscriberPath != null) {
      handler.setWebsocketPath(subscriberPath);
    }
    pipeline.addLast(new ChannelHandler[] { handler });
  }
}

