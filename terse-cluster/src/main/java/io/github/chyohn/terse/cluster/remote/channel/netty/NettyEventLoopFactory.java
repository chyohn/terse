package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;


public class NettyEventLoopFactory {
    static String NETTY_EPOLL_ENABLE_KEY = "netty.epoll.enable";
    static String OS_NAME_KEY = "os.name";
    static String OS_LINUX_PREFIX = "linux";

    static String OS_WIN_PREFIX = "win";
    static int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    static class NioEventLoopGroupHolder {
        final static EventLoopGroup group = eventLoopGroup(DEFAULT_IO_THREADS, "NettyClientWorker");
    }

    /**
     * netty client bootstrap
     *
     * @return event loop
     */
    public static EventLoopGroup getGlobalClientEventLoopGroup() {
        return NioEventLoopGroupHolder.group;
    }


    public static EventLoopGroup eventLoopGroup(int threads, String threadFactoryName) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName, true);
        return shouldEpoll()
                ? new EpollEventLoopGroup(threads, threadFactory)
                : new NioEventLoopGroup(threads, threadFactory);
    }

    public static Class<? extends SocketChannel> socketChannelClass() {
        return shouldEpoll() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return shouldEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    private static boolean shouldEpoll() {
        if (Boolean.parseBoolean(System.getProperty(NETTY_EPOLL_ENABLE_KEY, "false"))) {
            String osName = System.getProperty(OS_NAME_KEY);
            return osName.toLowerCase().contains(OS_LINUX_PREFIX) && Epoll.isAvailable();
        }

        return false;
    }
}
