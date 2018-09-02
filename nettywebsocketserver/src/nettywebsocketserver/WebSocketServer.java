package nettywebsocketserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.PrintStream;








public final class WebSocketServer
{
  static final boolean SSL = System.getProperty("ssl") != null;
  static final String PATH = System.getProperty("subscriberPath");
  static final int PORT = Integer.parseInt(System.getProperty("serverPort") != null ? System.getProperty("serverPort") : "8082");
  
  public WebSocketServer() {}
  
  public static void main(String[] args) throws Exception { SslContext sslCtx;
    SslContext sslCtxr;
    if (SSL) {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      sslCtxr = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    } else {
      sslCtxr = null;
    }
    
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      WebSocketServerInitializer initializer = new WebSocketServerInitializer(sslCtxr);
      initializer.setWebsockeSubscriberPath(PATH);
      ((ServerBootstrap)((ServerBootstrap)b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)).handler(new LoggingHandler(LogLevel.INFO))).childHandler(initializer);
      



      Channel ch = b.bind(PORT).sync().channel();
      
      System.out.println("Open your web browser and navigate to " + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');
      

      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
