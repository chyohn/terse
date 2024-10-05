package io.github.chyohn.terse.cluster.nio;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.nio.server.NIOServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


class NioServerTest {

    @Test
    void start() throws Exception {

        AtomicBoolean first = new AtomicBoolean(true);
        AtomicLong idg = new AtomicLong(0);
        new NIOServer(new InetSocketAddress("127.0.0.1", 9001), new ChannelHandler() {
            long start = 0;
            @Override
            public void connected(Channel channel) {
                System.out.println("new connect");
            }

            @Override
            public void disconnected(Channel channel) {
                System.out.println("dis connect");
            }

            @Override
            public void received(Channel channel, Object message) {

                if (true) {
                    if (first.compareAndSet(true, false)) {
                        start = System.currentTimeMillis();
                    }
                    System.out.println((System.currentTimeMillis() - start) + ": " + idg.getAndIncrement());
                    return;
                }
                ByteBuffer requestBuffer = (ByteBuffer)message;
                // id;;data;;;id;;data
                String s = new String(requestBuffer.array(), StandardCharsets.UTF_8);
                String[] ss = s.split(";;;");
                if (ss.length != 2) {
                    System.out.println("format error, not contain char comma, size: " + s.length() + " data array size: " + ss.length);
                    return;
                }
                if (!ss[0].equals(ss[1])) {
                    System.err.println("parse error：");
                } else {
                    String id = ss[0].split(";;", 2)[0];
                    System.out.println("true " + id + " t: " + Thread.currentThread().getName());

                    byte[] data = ss[0].getBytes(StandardCharsets.UTF_8);
                    channel.send(data);
                    System.out.println(id + " res size: " + data.length);
                }
            }

            @Override
            public void caught(Channel channel, Throwable throwable) {
                throwable.printStackTrace();
            }
        }).start();

        synchronized (NioServerTest.class) {
            NioServerTest.class.wait();
        }
    }
}