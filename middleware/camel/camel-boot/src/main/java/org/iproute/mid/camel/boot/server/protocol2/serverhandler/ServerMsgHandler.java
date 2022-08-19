package org.iproute.mid.camel.boot.server.protocol2.serverhandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.iproute.mid.camel.boot.server.protocol2.SimpleProtocol;

/**
 * ServerMsgHandler
 *
 * @author zhuzhenjie
 * @since 2022/8/19
 */
@Slf4j
public class ServerMsgHandler extends SimpleChannelInboundHandler<SimpleProtocol> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("ServerMsgHandler channelActive");

        String msg = "Netty,Rock!";

        SimpleProtocol pMsg = SimpleProtocol.builder()
                .len(msg.getBytes().length)
                .content(msg.getBytes())
                .build();

        ctx.writeAndFlush(pMsg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SimpleProtocol msg) throws Exception {
        String clientMsg = new String(msg.getContent());
        SocketChannel channel = (SocketChannel) ctx.channel();
        log.info("接收到客户端消息|{}|{}", channel.remoteAddress().toString(), clientMsg);

        String rspMsg = "server response ： " + clientMsg;
        SimpleProtocol pRspMsg = SimpleProtocol.builder()
                .len(rspMsg.getBytes().length)
                .content(rspMsg.getBytes())
                .build();

        ctx.writeAndFlush(pRspMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("exceptionCaught|{}", cause.getMessage());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        log.info("channelInactive|{}", channel.remoteAddress().toString());
    }

}