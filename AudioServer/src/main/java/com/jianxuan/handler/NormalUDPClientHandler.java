package com.jianxuan.handler;

import com.jianxuan.content.ConnectContent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public class NormalUDPClientHandler extends ChannelInboundHandlerAdapter {
	private  Lock lock = new ReentrantLock();
	private  Condition condition = lock.newCondition();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        L.d(ctx.channel().remoteAddress() + "");
        ctx.executor().parent().execute(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					lock.lock();
					try {
						//当成功连接上以后 进行阻塞
						if(ConnectContent.isConnect){
							condition.await();
						}

						// 发送本端ip地址
						// 获取所有网络接口
						Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
						for (NetworkInterface networkInterface : Collections.list(interfaces)) {
							// 检查网络接口是否开启并支持广播
							if (networkInterface.isUp() && networkInterface.supportsMulticast()) {
								for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
									InetAddress broadcast = interfaceAddress.getBroadcast();
									if (broadcast == null) {
										continue;
									}
									// 发送数据包到网络接口的广播地址
									log.info("广播发送，当前接口："+broadcast.getHostAddress());
									ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(interfaceAddress.getAddress().toString().substring(1), Charset.forName("UTF-8")),
											new InetSocketAddress(broadcast, 10000)));
								}
							}
						}
//						ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(InetAddress.getLocalHost().getHostAddress(), Charset.forName("utf-8"))
//								, new InetSocketAddress("255.255.255.255", 10000)));
					} catch (InterruptedException | SocketException e) {
                        e.printStackTrace();
					}finally {
						lock.unlock();
					}
				}
			}
		});

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        L.d(ctx.channel().remoteAddress() + "");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf byteBuf = packet.copy().content();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String content = new String(bytes);
//        L.d(packet.sender()+","+content);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
	}
}

