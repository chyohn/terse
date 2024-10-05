/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.cluster.broadcast;


/**
 * 集群消息广播
 *
 * @author : qiang.shao
 * @since 1.0.0
 */
public interface IBroadcaster {

    /**
     * 订阅广播消息
     *
     * @param topic    广播消息topic
     * @param receiver 广播消息处理器
     */
    void subscribe(String topic, BroadcastMessageReceiver receiver);

    /**
     * 发送广播消息
     *
     * @param topic   广播消息topic
     * @param message 消息体内容
     */
    void broadcast(String topic, String message);

}
