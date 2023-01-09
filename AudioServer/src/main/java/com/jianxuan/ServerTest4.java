package com.jianxuan;

import com.jianxuan.config.Config;
import com.jianxuan.content.ConnectContent;
import com.jianxuan.handler.NormalUDPClientHandler;
import com.jianxuan.message.AudioMessage;
import com.jianxuan.protocol.MessageCodecSharable;
import com.jianxuan.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class ServerTest4 {
    private static TargetDataLine targetDataLine;
    private static AudioFormat format = new AudioFormat(Config.getSampleRate(), Config.getSampleBits(), Config.getSampleChannels(), true, false);
    public static NormalUDPClientHandler udpHandler = new NormalUDPClientHandler();
    public static void main(String[] args) throws UnknownHostException {
        System.out.println("**********麦克风同传软件*****************");
        System.out.println("*********请勿关闭本窗口！！！*************");
        System.out.println("********传递403麦克风声音到405***********");
        System.out.println("***********by 剑轩*********************");
        System.out.println("***********版本 V2*********************");
        System.out.println("***** 本端ip地址为："+InetAddress.getLocalHost().getHostAddress()+" ****");
        log.debug("启动中");
        // 广播udp 处理
        udpBroadcast();
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        try {
            log.debug("等待连接建立");
            // 初始化资源线
            targetDataLine = AudioSystem.getTargetDataLine(format);
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(
                    new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            log.debug("建立连接");
                            ConnectContent.isConnect = true ;
//                            ch.pipeline().addLast(LOGGING_HANDLER);
                            ch.pipeline().addLast(new ProcotolFrameDecoder());
                            ch.pipeline().addLast(MESSAGE_CODEC);
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    log.debug("连接断开");
                                    ConnectContent.isConnect = false ;
                                    udpHandler.getLock().lock();
                                    udpHandler.getCondition().signal();
                                    udpHandler.getLock().unlock();
                                    targetDataLine.close();
                                    ctx.channel().close().sync();
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    new Thread(() -> getAudio_A(ctx),"系统录制").start();
//                                     ctx.writeAndFlush(new AudioMessage(100,new byte[]{1,2,3,4}));
                                }
                            });
                        }
                    });
            Channel channel = serverBootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    //录制音频
    public static void getAudio_A(ChannelHandlerContext ctx) {//提供wave格式信息
        try {
            targetDataLine.open(format);
            targetDataLine.start();
            //直接播放出来
            //开子线程进行播放
            byte[] bytes = new byte[1024];//缓存音频数据
                    for(;;){
                        int a = 0;
                        a = targetDataLine.read(bytes, 0, bytes.length);//捕获录音数据
                        AudioMessage message = new AudioMessage(a, bytes);
                        ctx.writeAndFlush(message).sync();//播放录制的声音
                    }
        }catch (LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void udpBroadcast() {
        new Thread(() -> {
            log.info("广播开始");
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap()
                        .group(eventLoopGroup)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(udpHandler);

                bootstrap.bind(9999).sync().channel().closeFuture().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                eventLoopGroup.shutdownGracefully();
            }
        },"广播").start();

    }

}
