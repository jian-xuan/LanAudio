package com.jianxuan;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.sound.sampled.*;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) {
        getAudio_A();
    }


    //录制音频
    public static void getAudio_A() {//提供wave格式信息
        try {
            AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
            TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(format);
            targetDataLine.open(format);
            targetDataLine.start();
            System.out.println("录音中");
            //直接播放出来
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
            sourceDataLine.open(format);
            sourceDataLine.start();
            System.out.println("播放中");
            //开子线程进行播放
            byte[] b = new byte[1024];//缓存音频数据
            new Thread(new Runnable() {
                public void run() {
                    int a = 0;
                    while (a != -1) {
                        System.out.println("录制中");
                        a = targetDataLine.read(b, 0, b.length);//捕获录音数据
                        if (a != -1) {
                            sourceDataLine.write(b, 0, a);//播放录制的声音
                        }
                    }
                }
            }).start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
    public void test() {
        public class TcpClient {
            private volatile EventLoopGroup workerGroup;
            private volatile Bootstrap bootstrap;
            private volatile boolean closed = false;
            private final String remoteHost;
            private final int remotePort;

            public TcpClient(String remoteHost, int remotePort) {
                this.remoteHost = remoteHost;
                this.remotePort = remotePort;
            }

            public void close() {
                closed = true;
                workerGroup.shutdownGracefully();
                System.out.println("Stopped Tcp Client: " + getServerInfo());
            }

            public void init() {
                closed = false;
                workerGroup = new NioEventLoopGroup();
                bootstrap = new Bootstrap();
                bootstrap.group(workerGroup);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addFirst(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                ctx.channel().eventLoop().schedule(() -> doConnect(), 1, TimeUnit.SECONDS);
                            }
                        });                  //todo: add more handler
                    }
                });
                doConnect();
            }

            private void doConnect() {
                if (closed) {
                    return;
                }
                ChannelFuture future = bootstrap.connect(new InetSocketAddress(remoteHost, remotePort));
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (f.isSuccess()) {
                            System.out.println("Started Tcp Client: " + getServerInfo());
                        } else {
                            System.out.println("Started Tcp Client Failed: " + getServerInfo());
                            f.channel().eventLoop().schedule(() -> doConnect(), 1, TimeUnit.SECONDS);
                        }
                    }
                });
            }

            private String getServerInfo() {
                return String.format("RemoteHost=%s RemotePort=%d", remotePort, remotePort);
            }
        }
    }
**/

}
