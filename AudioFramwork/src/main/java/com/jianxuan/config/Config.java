package com.jianxuan.config;



import com.jianxuan.protocol.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public abstract class Config {
    static Properties properties;
    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static  int getSampleChannels(){
        String value = properties.getProperty("sampleChannels");
        if(value == null) {
            return 1;
        } else {
            return Integer.parseInt(value);
        }
    }


    public static  int getSampleBits(){
        String value = properties.getProperty("sampleBits");
        if(value == null) {
            return 16;
        } else {
            return Integer.parseInt(value);
        }
    }



    public static  int getSampleRate(){
        String value = properties.getProperty("sampleRate");
        if(value == null) {
            return 9600;
        } else {
            return Integer.parseInt(value);
        }
    }

    public static int getServerPort() {
        String value = properties.getProperty("server.port");
        if(value == null) {
            return 8080;
        } else {
            return Integer.parseInt(value);
        }
    }

    public static String getIpAddress() {
        String ip = properties.getProperty("ip.address");
        if(ip == null) {
           log.error("ip地址错误");
        }else {
            return ip ;
        }
        return "localhost";
    }

    public static Serializer.Algorithm getSerializerAlgorithm() {
        String value = properties.getProperty("serializer.algorithm");
        if(value == null) {
            return Serializer.Algorithm.Json;
        } else {
            return Serializer.Algorithm.valueOf(value);
        }
    }
}