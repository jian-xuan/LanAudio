package com.jianxuan.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.sound.sampled.*;

public class RecordHandler extends SimpleChannelInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    }
    //录制音频
    private void getAudio_A() {//提供wave格式信息
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
                    while(a!=-1) {
                        System.out.println("录制中");
                        a = targetDataLine.read(b, 0, b.length);//捕获录音数据
                        if(a!=-1) {
                            sourceDataLine.write(b, 0, a);//播放录制的声音
                        }
                    }
                }
            }).start();
        }catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
