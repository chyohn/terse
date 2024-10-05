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

package io.github.chyohn.terse.cluster;

import java.util.Map;

import io.github.chyohn.terse.cluster.broadcast.IBroadcaster;
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.spi.SPI;

/**
 * 提供集群服务能力
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@SPI(allowMultiInstance = false)
public interface IClusterClient {

    /**
     * when system init, should init cluster and client
     *
     * @param config cluster config
     */
    void onInit(Map<String, Object> config);
    /**
     * whether cluster is init, if true, can send cluster request
     *
     * @return if true, client can send cluster request
     */
    boolean isInit();

    /**
     * when all service are ready, start to receive service request from cluster.
     */
    void onReady();

    /**
     * whether cluster is started, if true, can receive cluster request
     *
     * @return if true, client can receive cluster request
     */
    boolean isReady();

    /**
     * 从集群中获取请求结果
     *
     * @param request  请求对象
     * @param callable 请求结果回调
     */
    void request(ICommand request, Callback2<IResult<?>, Throwable> callable);

    /**
     * 获取广播器
     *
     * @return a broadcaster
     */
    IBroadcaster getBroadcaster();


}
