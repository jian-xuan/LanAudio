package com.jianxuan;

import com.jianxuan.config.Config;
import com.jianxuan.protocol.MessageCodecSharable;
import com.jianxuan.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
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
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ClientTest {
    public static void main(String[] args) throws LineUnavailableException {
        LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(Config.getSampleRate(), Config.getSampleBits(), Config.getSampleChannels(), true, false);
                SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
                sourceDataLine.open(format);
                sourceDataLine.start();
                for(;;){
                      if(queue.size()>20) {
                          byte[] bytes = queue.take();
                          sourceDataLine.write(bytes, 0,bytes.length);//播放录制的声音
                      }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 黏包半包
//                    ch.pipeline().addLast(new ProcotolFrameDecoder());
//                    ch.pipeline().addLast(MESSAGE_CODEC);
//                    ch.pipeline().addLast(LOGGING_HANDLER);
//                    ch.pipeline().addLast(new IdleStateHandler(0,3,0));
                    ch.pipeline().addLast("client handler", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            ByteBuf byteBuf = (ByteBuf) msg;
                            byte[] bytes = new byte[1024*1024];
                            byteBuf.readBytes(bytes);
                            queue.put(bytes);
                        }
                    });
                }
            });
            Channel channel = bootstrap.connect(new InetSocketAddress(Config.getIpAddress(),8080)).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
    //录制音频
    public static void getAudio_A() {//提供wave格式信息
        byte[] bytes = message.getData();
        int size = message.getSize();
        //直接播放出来
//        System.out.println("播放中");
        sourceDataLine.write(bytes, 0,size);//播放录制的声音
        //开子线程进行播放
    } **/
}
