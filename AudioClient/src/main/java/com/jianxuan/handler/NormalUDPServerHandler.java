package com.jianxuan.handler;

import com.jianxuan.content.IpContent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public class NormalUDPServerHandler extends ChannelInboundHandlerAdapter {
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf byteBuf = packet.copy().content();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String content = new String(bytes);
        IpContent.ipAdd = content;
        log.debug("广播收到的远端信息为"+content);
        // 当成功连接时 释放资源
        if(IpContent.ipAdd != null) {
            lock.lock();
            condition.await();
            lock.unlock();
        }
        // 可以发送一些ack的数据
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
	}
}
