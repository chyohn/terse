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

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.ICommandInvoker;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.exception.CommandExecuteException;
import io.github.chyohn.terse.exception.TimeoutException;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IReliableTask;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.utils.ObjectUtils;
import io.github.chyohn.terse.flow.ITaskHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 流程执行入口方法{@link #execute()}
 * <ol>
 * <li>维护各个执行节点的执行顺序。节点满足下面的任意条件就可立即执行
 *  <ul>
 *      <li>节点无依赖节点 </li>
 *      <li>节点的所有依赖节点都执行完成 </li>
 *  </ul>
 * </li>
 * <li>保证每个节点的处理不存在多线程竞争。 由于结果从不同的线程返回来，为了避免多线程竞争，则使用了一个队列来存放结果，
 *  并由execute()方法的执行线程来消费队列并处理结果。这样一来，{@link ITaskHandler}对象的所有操作都在同一个线程中执行，避免了线程同步操作。
 * </li>
 * </ol>
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Slf4j
final class NodeScheduler {

    // 响应结果同步队列
    private final BlockingQueue<CommandFinishedMessage> commandFinishedQueue;

    private final ICommandInvoker commandInvoker;
    private final IFlowContext context;
    private final RunningWay runningWay;
    // 记录正在运行的节点数据
    private int nodeRunningCount = 0;
    private DagGraph dagGraph;

    NodeScheduler(IReliableTask<?> summaryTask, IFlowContext context, RunningWay runningWay) {
        this.context = context;
        this.runningWay = runningWay;
        commandInvoker = ISpiFactory.get(ICommandInvoker.class);
        // 构建DAG图
        dagGraph = DagGraphGenerator.generateDAGByLevel(summaryTask);

        // 创建任务结果优先处理队列
        int queueSize = dagGraph.getSize() > Integer.MAX_VALUE>>2 ? Integer.MAX_VALUE : Math.max(dagGraph.getSize()<<2, 512);
        commandFinishedQueue = new PriorityBlockingQueue<>(queueSize, Comparator.comparingInt(msg -> msg.node.getChildrenMinLevel()));
    }

    /**
     * 执行排序
     */
    void execute() {

        // 从根节点开始执行
        startFromRoot();

        try {
            // 等到所有节点执行完成
            waitAllNodesFinish();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandExecuteException(e);
        }
    }

    private void startFromRoot() {
        // 移除root节点的引用，以便GC能处理
        List<Node> roots = dagGraph.removeRoot();
        // 对节点的执行顺序做排序
        roots.sort(NodeExecuteComparator.getInstance());
        for (Node root : roots) {
            runNode(root);
        }
    }

    private void waitAllNodesFinish() throws InterruptedException {
        CommandFinishedMessage message = null;
        while (nodeRunningCount > 0) {
            if (context.timeout() > 0) {
                message = commandFinishedQueue.poll(context.timeout(), TimeUnit.MILLISECONDS);
            } else {
                message = commandFinishedQueue.take();
            }

            if (message == null) {
                throw new TimeoutException("执行节点超时, timeout: " + context.timeout());
            }

            // 处理结果
            Node node = message.node;
            IResult<?> result = message.result;
            if (node.handleResult(result)) {
                nodeFinished(node);
                nodeRunningCount--;
            }
        }
    }


    /**
     * 节点执行完成
     *
     * @param finishedNode 已完成节点
     */
    private void nodeFinished(Node finishedNode) {

        List<Node> children = finishedNode.getChildren();
        children.sort(NodeExecuteComparator.getInstance());

        // 执行下一个节点
        for (Node child : children) {
            if (child.canExecute(finishedNode)) {
                runNode(child);
            }
        }
    }


    /**
     * 执行节点
     */
    private void runNode(Node node) {

        // 执行节点
        List<ICommand> commands = node.execute(context);
        if (ObjectUtils.isEmpty(commands)) {
            nodeFinished(node);
            return;
        }

        // 提交节点的异步命令
        this.nodeRunningCount++;
        IReliableTask<IFlowContext> reliableNode = node.getTask();
        commandInvoker.asyncInvoke(commands, reliableNode.getTimeout(), this.runningWay,
                res -> sendFinishedMessage(node, res));
    }

    /**
     * 结果获取完成发送完成通知
     */
    private void sendFinishedMessage(Node node, IResult<?> result) {
        try {
            commandFinishedQueue.put(new CommandFinishedMessage(node, result));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * 用于同步队列存储节点的响应结果
     */
    private static final class CommandFinishedMessage {

        final IResult<?> result;
        final Node node;

        CommandFinishedMessage(Node node, IResult<?> result) {
            this.result = result;
            this.node = node;
        }
    }

}
