package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * <pre>
 * |--------------------------------------------------|
 * |模数3字节|序号4字节|总序号4字节|长度4字节|body|尾部4字节|
 * |--------------------------------------------------|
 * </pre>
 */
public class CodecAdaptor {

    public static final CodecAdaptor INST = new CodecAdaptor();
    private static final int HEADER_LENGTH = 4;
    private static final int MAX_COUNT_PER_READ = 10;


    public ChannelOutboundHandler newEncoder() {
        return new ObjectToByteEncoder();
    }

    public ChannelInboundHandler newDecoder() {
        return new ByteToObjectDecoder();
    }

    private static class ObjectToByteEncoder extends MessageToByteEncoder<Object> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

            ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
            if (msg instanceof byte[]) {
                byte[] data = (byte[]) msg;
                header.putInt(data.length);
                header.rewind();
                sendBuffer(out, header, ByteBuffer.wrap(data));
                return;
            } else if (msg instanceof ByteBuffer) {
                ByteBuffer buffer = (ByteBuffer) msg;
                header.putInt(buffer.capacity());
                header.rewind();
                sendBuffer(out, header, buffer);
                return;
            }

            if (msg instanceof Iterable) {
                for (Object o : (Iterable) msg) {
                    encode(ctx, o, out);
                }
                return;
            }

            throw new IllegalArgumentException("can't support message class type: " + msg.getClass());
        }

        private void sendBuffer(ByteBuf out, ByteBuffer... buffers) {
            for (ByteBuffer buffer : buffers) {
                out.writeBytes(buffer);
            }
        }

    }



    private static class ByteToObjectDecoder extends ByteToMessageDecoder {
        private final ByteBuffer lenBuffer = ByteBuffer.allocate(HEADER_LENGTH);
        int readLength = HEADER_LENGTH;
        private ByteBuffer incomingBuffer = lenBuffer;

        private void reset() {
            lenBuffer.clear();
            incomingBuffer = lenBuffer;
            readLength = HEADER_LENGTH;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

            while (in.readableBytes() >= readLength && out.size() <= MAX_COUNT_PER_READ) {
                if (incomingBuffer == null) {
                    // new data buffer
                    incomingBuffer = ByteBuffer.allocate(readLength);
                }

                // read length or data
                in.readBytes(incomingBuffer);
                if (incomingBuffer == lenBuffer) {
                    // get length
                    lenBuffer.flip();
                    readLength = lenBuffer.getInt();
                    if (readLength < 0) {
                        int len = readLength;
                        reset();
                        throw new IOException("Len error. "
                                + "A message from with advertised length of " + len);
                    }
                    incomingBuffer = null;
                    continue;
                }

                // complete read data
                incomingBuffer.flip();
                out.add(incomingBuffer);

                // reset
                reset();
            }

        }
    }



}
