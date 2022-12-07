package com.jianxuan;

import com.jianxuan.config.Config;
import com.jianxuan.message.AudioMessage;
import com.jianxuan.protocol.MessageCodecSharable;
import com.jianxuan.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientTest3 {
    public static void main(String[] args) throws LineUnavailableException {
        System.out.println("**********麦克风同传软件*****************");
        System.out.println("*********请勿关闭本窗口！！！*************");
        System.out.println("********传递403麦克风声音到405***********");
        System.out.println("*************by 剑轩*******************");
        System.out.println("*************版本 V2*******************");
        System.out.println("********** 服务端ip地址为："+Config.getIpAddress()+" *******************");
        System.out.println("**如无法连接请检查ip地址\n**并在本jar包中application.properties文件修改****");
        // 创建线程共享的队列
        LinkedBlockingQueue<AudioMessage> queue = new LinkedBlockingQueue<>();
        new Thread(() -> {
            try{
                log.debug("初始化资源....");
                //初始化播放的资源
                AudioFormat format = new AudioFormat(Config.getSampleRate(), Config.getSampleBits(), Config.getSampleChannels(), true, false);
                SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
                sourceDataLine.open(format);
                sourceDataLine.start();
                for(;;){
                    if(queue.size()>10) {
                        for(int i=0;i<8;i++){
                            AudioMessage msg = queue.take();
                            //播放音频
                            sourceDataLine.write(msg.getData(), 0,msg.getSize());
                        }
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        log.debug("启动中");
        //解码器
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class)
            .group(group)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 黏包半包
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast("reconnect",new ChannelInboundHandlerAdapter(){
                        /**
                         * 当检测到服务端连接断开时，进行重连
                         * @param ctx
                         * @throws Exception
                         */
                        @Override
                        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                            log.debug("连接断开:" + ctx.channel().remoteAddress());
                            super.channelInactive(ctx);
                            //进行重连
                            ChannelFuture channelFuture = connectAndReconnect(bootstrap);
                        }
                    });

                    ch.pipeline().addLast("client handler", new SimpleChannelInboundHandler<AudioMessage>() {
                        /**
                         * 处理收到的数据，并将其放进队列中供播放线程取
                         * @param ctx
                         * @param msg
                         * @throws Exception
                         */
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, AudioMessage msg) throws Exception {
//                            System.out.println(msg.toString());
                            log.debug("收到msg");
                            queue.put(msg);
                        }
                    });
                }
            });
            ChannelFuture channelFuture = connectAndReconnect(bootstrap);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            // 根据需求 在此线程无需关闭资源，方便后续的重连
//            group.shutdownGracefully();
        }
    }

    /**
     * 连接和重连
     * @param bootstrap
     * @return
     * @throws InterruptedException
     */
    public static ChannelFuture connectAndReconnect(Bootstrap bootstrap) throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(Config.getIpAddress(), 8080)).await();
        //不断轮询进行重连
        while (!channelFuture.isSuccess()) {
//            log.debug("不成功");
            channelFuture = bootstrap.connect(new InetSocketAddress(Config.getIpAddress(), 8080)).await();
        }
        return channelFuture;
    }

}
