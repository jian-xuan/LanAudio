package com.jianxuan;

import com.jianxuan.config.Config;
import com.jianxuan.message.AudioMessage;
import com.jianxuan.protocol.MessageCodecSharable;
import com.jianxuan.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ClientTest2 {
    public static void main(String[] args) throws LineUnavailableException {
        LinkedBlockingQueue<AudioMessage> queue = new LinkedBlockingQueue<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    log.debug("初始化资源....");
                    AudioFormat format = new AudioFormat(Config.getSampleRate(), Config.getSampleBits(), Config.getSampleChannels(), true, false);
                    SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
                    sourceDataLine.open(format);
                    sourceDataLine.start();
                    for(;;){
                        if(queue.size()>10) {
                            for(int i=0;i<8;i++){
                                AudioMessage msg = queue.take();
//                                System.out.println(queue.size());
//                        if(msg !=null)
                                sourceDataLine.write(msg.getData(), 0,msg.getSize());
                            }
                        }

                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
        log.debug("启动中");
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);


        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 黏包半包
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    ch.pipeline().addLast(MESSAGE_CODEC);
//                    ch.pipeline().addLast(LOGGING_HANDLER);
//                    ch.pipeline().addLast(new IdleStateHandler(0,3,0));
                    ch.pipeline().addLast("client handler", new SimpleChannelInboundHandler<AudioMessage>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, AudioMessage msg) throws Exception {
//                            System.out.println(msg.toString());
                            log.debug("收到msg");
                            queue.put(msg);
                        }
                    });
                }
            });
            Channel channel = bootstrap.connect(new InetSocketAddress(Config.getIpAddress(),8080)).sync().channel();
//            System.out.println(1);
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }

    //录制音频
    public static void getAudio_A(SourceDataLine sourceDataLine, AudioMessage message) {//提供wave格式信息
        byte[] bytes = message.getData();
        int size = message.getSize();
        //直接播放出来
//        System.out.println("播放中");
        sourceDataLine.write(bytes, 0,size);//播放录制的声音
        //开子线程进行播放
    }
}
