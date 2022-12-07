package com.jianxuan;

import com.jianxuan.config.Config;
import com.jianxuan.message.AudioMessage;
import com.jianxuan.protocol.MessageCodecSharable;
import com.jianxuan.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;


@Slf4j
public class Server {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(
                    new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            log.debug("建立连接");
//                            ch.pipeline().addLast(LOGGING_HANDLER);
                            ch.pipeline().addLast(new ProcotolFrameDecoder());
                            ch.pipeline().addLast(MESSAGE_CODEC);
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    getAudio_A(ctx);
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
            AudioFormat format = new AudioFormat(Config.getSampleRate(), Config.getSampleBits(), Config.getSampleChannels(), true, false);
            TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(format);
            System.out.println(format.getEncoding());
            targetDataLine.open(format);
            targetDataLine.start();
            //直接播放出来
            //开子线程进行播放
            byte[] bytes = new byte[1024];//缓存音频数据
            new Thread(new Runnable() {
                public void run() {
                    int a = 0;
                    while(a!=-1) {
//                        System.out.println("录制中");
                        a = targetDataLine.read(bytes, 0, bytes.length);//捕获录音数据
//                        System.out.println(a);
                        if(a!=-1) {
                            AudioMessage message = new AudioMessage(a, bytes);
                            ctx.writeAndFlush(message);//播放录制的声音
                        }
                    }
                }
            }).start();
        }catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

}
