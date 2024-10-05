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

package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.flow.ITaskHandler;


/**
 * 记录节点的执行信息 节点的所有运行时数据都在该类中获取
 *
 * @author qiang.shao
 * @since 1.0.0
 */
class NodeExecuteInfo {

    final ITaskHandler handler; // 结果处理对象
    int commandSize; // 需要执行的请求数
    boolean notHaveCommand;

    NodeExecuteInfo(ITaskHandler handler, int commandSize) {
        // 获取特征及请求
        this.handler = handler;
        this.commandSize = commandSize;
        this.notHaveCommand = commandSize == 0;
    }


    /**
     * true 无请求对象
     */
    boolean notHaveCommand() {
        return this.notHaveCommand;
    }

    /**
     * 执行完成一个请求加1
     */
    void incrFinish() {
        if (this.commandSize > 0) {
            this.commandSize--;
        }
    }

    /**
     * true: 所有请求都完成
     */
    boolean isAllFinished() {
        return this.commandSize == 0;
    }

}
