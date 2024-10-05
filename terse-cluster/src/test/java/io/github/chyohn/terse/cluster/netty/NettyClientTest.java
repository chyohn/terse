package io.github.chyohn.terse.cluster.netty;

import io.github.chyohn.terse.cluster.remote.channel.Channel;
import io.github.chyohn.terse.cluster.remote.channel.ChannelHandler;
import io.github.chyohn.terse.cluster.remote.channel.netty.NettyClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

class NettyClientTest {

    @Test
    void testC() throws Exception {
        int tn = 1;
        for (int i = 0; i < tn; i++) {
            test();
        }
        synchronized (NettyClientTest.class) {
            NettyClientTest.class.wait();
        }
    }

    void test() throws Exception {
        NettyClient client = new NettyClient(new InetSocketAddress("127.0.0.1", 9001), new ChannelHandler() {
            @Override
            public void connected(Channel channel) {
                System.out.println("connect success");
            }

            @Override
            public void disconnected(Channel channel) {
                System.out.println("connect closed");
            }

            @Override
            public void received(Channel channel, Object message) {
                ByteBuffer buffer = (ByteBuffer) message;
                String[] ss = new String(buffer.array(), StandardCharsets.UTF_8).split(";;", 2);
                if (ss.length != 2) {
                    System.out.println("xxxxxx");
                    return;
                }
                System.out.println(Thread.currentThread().getName() + " recevie id: " + ss[0]);
            }

            @Override
            public void caught(Channel channel, Throwable throwable) {
                throwable.printStackTrace();
            }


        });
        client.start();

        AtomicLong idg = new AtomicLong(0);
        new Thread(() -> {
            while (!client.isClosed()) {
                try {
                    if (!client.isConnected()) {
                        Thread.sleep(1000);
                        continue;
                    }
                    Random random = new Random();
                    long id = idg.getAndIncrement();
                    String t = id + ";;" + genString(random.nextInt(1024));
                    String msg = t + ";;;" + t;
                    byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                    ByteBuffer rb = ByteBuffer.wrap(data);
                    client.send(rb);
                    System.out.println(id + "send size: " + data.length);
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private static String genString(int size) {
        Random random = new Random();
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

}