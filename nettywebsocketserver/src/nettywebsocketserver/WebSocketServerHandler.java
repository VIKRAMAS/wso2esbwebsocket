package nettywebsocketserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import java.io.PrintStream;


public class WebSocketServerHandler
  extends SimpleChannelInboundHandler<Object>
{
  public static String WEBSOCKET_PATH = "/websocket";
  private WebSocketServerHandshaker handshaker;
  
  public WebSocketServerHandler() {}
  
  public void setWebsocketPath(String path) { WEBSOCKET_PATH = path; }
  

  public void channelRead0(ChannelHandlerContext ctx, Object msg)
  {
    if ((msg instanceof FullHttpRequest)) {
      handleHttpRequest(ctx, (FullHttpRequest)msg);
    } else if ((msg instanceof WebSocketFrame)) {
      handleWebSocketFrame(ctx, (WebSocketFrame)msg);
    }
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx)
  {
    ctx.flush();
  }
  
  private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)
  {
    if (!req.getDecoderResult().isSuccess()) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
      return;
    }
    

    if (req.getMethod() != HttpMethod.GET) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
      return;
    }
    

    if ("/".equals(req.getUri())) {
      ByteBuf content = WebSocketServerIndexPage.getContent(getWebSocketLocation(req));
      FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
      
      res.headers().set("Content-Type", "text/html; charset=UTF-8");
      HttpHeaders.setContentLength(res, content.readableBytes());
      
      sendHttpResponse(ctx, req, res);
      return;
    }
    if ("/favicon.ico".equals(req.getUri())) {
      FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
      sendHttpResponse(ctx, req, res);
      return;
    }
    

    WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), "synapse(contentType='application/xml'),synapse(contentType='application/json')", true);
    

    handshaker = wsFactory.newHandshaker(req);
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
    } else {
      handshaker.handshake(ctx.channel(), req);
    }
  }
  

  private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
  {
    if ((frame instanceof CloseWebSocketFrame)) {
      handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());
      return;
    }
    if ((frame instanceof PingWebSocketFrame)) {
      ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
      return;
    }
    if (!(frame instanceof TextWebSocketFrame)) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", new Object[] { frame.getClass().getName() }));
    }
    


    String request = ((TextWebSocketFrame)frame).text();
    if ((handshaker.selectedSubprotocol() != null) && (handshaker.selectedSubprotocol().contains("synapse(contentType='application/xml')")))
    {
      System.err.printf("%s received %s%n", new Object[] { ctx.channel(), request });
      ctx.channel().write(new TextWebSocketFrame("<websocketResponse>dummy</websocketResponse>"));
    } else if ((handshaker.selectedSubprotocol() != null) && (handshaker.selectedSubprotocol().contains("synapse(contentType='application/json')")))
    {
      System.err.printf("%s received %s%n", new Object[] { ctx.channel(), request });
      ctx.channel().write(new TextWebSocketFrame("{\"websocketResponse\":\"dummy\"}"));
    } else {
      System.err.printf("%s received %s%n", new Object[] { ctx.channel(), request });
      ctx.channel().write(new TextWebSocketFrame(request));
    }
  }
  

  private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res)
  {
    if (res.getStatus().code() != 200) {
      ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
      res.content().writeBytes(buf);
      buf.release();
      HttpHeaders.setContentLength(res, res.content().readableBytes());
    }
    

    ChannelFuture f = ctx.channel().writeAndFlush(res);
    if ((!HttpHeaders.isKeepAlive(req)) || (res.getStatus().code() != 200)) {
      f.addListener(ChannelFutureListener.CLOSE);
    }
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    cause.printStackTrace();
    ctx.close();
  }
  
  private static String getWebSocketLocation(FullHttpRequest req) {
    String location = req.headers().get("Host") + WEBSOCKET_PATH;
    if (WebSocketServer.SSL) {
      return "wss://" + location;
    }
    return "ws://" + location;
  }
}
