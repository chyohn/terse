/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.chyohn.terse.cluster.remote.channel.netty;

import io.github.chyohn.terse.cluster.utils.BatchExecutorQueue;
import io.netty.channel.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class NettyBatchWriteQueue extends BatchExecutorQueue<NettyBatchWriteQueue.MessageTuple> {

    public static NettyBatchWriteQueue createWriteQueue(Channel channel) {
        return new NettyBatchWriteQueue(channel);
    }
    private final Channel channel;

    private final Queue<ChannelPromise> promises = new LinkedList<>();
    private final List<Object> messages = new ArrayList<>();

    private NettyBatchWriteQueue(Channel channel) {
        this.channel = channel;
    }

    public ChannelFuture enqueue(Object message) {
        return enqueue(message, channel.newPromise());
    }

    public ChannelFuture enqueue(Object message, ChannelPromise channelPromise) {
        MessageTuple messageTuple = new MessageTuple(message, channelPromise);
        super.enqueue(messageTuple, channel.eventLoop());
        return messageTuple.channelPromise;
    }

    @Override
    protected void prepare(MessageTuple item) {
        messages.add(item.originMessage);
        promises.add(item.channelPromise);
    }

    @Override
    protected void flush() {

        Object finalMessage = messages;
        if (messages.size() == 1) {
            finalMessage = messages.get(0);
        }

        channel.writeAndFlush(finalMessage).addListener(future -> {
            ChannelPromise cp;
            while ((cp = promises.poll()) != null) {
                if (future.isSuccess()) {
                    cp.setSuccess();
                } else {
                    cp.setFailure(future.cause());
                }
            }
        });

        messages.clear();
    }


    public static class MessageTuple {

        private final Object originMessage;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object originMessage, ChannelPromise channelPromise) {
            this.originMessage = originMessage;
            this.channelPromise = channelPromise;
        }
    }
}
