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

import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IReliableTask;
import io.github.chyohn.terse.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Slf4j
class DagGraphGenerator {

    private static final DagGraphGenerator INSTANCE = new DagGraphGenerator();
    public static DagGraph generateDAGByLevel(IReliableTask<?> endTask) {
        return INSTANCE.doGenerateDAGByLevel(endTask);
    }

    @SuppressWarnings("unchecked")
    private DagGraph doGenerateDAGByLevel(IReliableTask<?> endTask) {

        IReliableTask<IFlowContext> task = (IReliableTask<IFlowContext>) endTask;

        Map<IReliableTask<IFlowContext>, Node> nodeOfTask = new HashMap<>();
        // 计算特征的依赖
        generateTaskNodes(nodeOfTask, task, new HashSet<>());

        DagGraph graph = new DagGraph();
        // 获取根节点
        List<Node> root = nodeOfTask.values().stream()
                .filter(node -> computeAndGetNodeLevel(node) == Node.ROOT_LEVEL)
                .collect(Collectors.toList());
        graph.setRoot(root);
        graph.setSize(nodeOfTask.size());
        return graph;
    }

    interface Generator {
        void gen(List<? extends IReliableTask<IFlowContext>> src, Consumer<Node> consumer);
    }

    private void generateTaskNodes(Map<IReliableTask<IFlowContext>, Node> nodeOfTask, IReliableTask<IFlowContext> task, Set<Node> initialNodes) {

        if (nodeOfTask.containsKey(task)) {
            return;
        }

        Node node = new Node(task);
        nodeOfTask.put(task, node);

        Generator generator = (parents, c) -> {
            if (ObjectUtils.isEmpty(parents)) {
                return;
            }
            for (IReliableTask<IFlowContext> parent : parents) {
                // 递归计算父特征的依赖
                generateTaskNodes(nodeOfTask, parent, initialNodes);
                Node parentNode = nodeOfTask.get(parent);
                if (initialNodes.contains(parentNode)) {
                    throw new IllegalStateException(buildCycleMsg(node.getTask(), parent));
                }
                c.accept(parentNode);
            }
        };

        initialNodes.add(node);
        generator.gen(task.randomConditions(), node::addRandomConditions);
        generator.gen(task.mustConditions(), node::addMustConditions);
        generator.gen(task.randomRelyOnTasks(), node::addRandomRelyOnTasks);
        generator.gen(task.mustRelyOnTasks(), node::addMustRelyOnTasks);
        initialNodes.remove(node);

        node.setExecutor(NodeExecutorFactory.getExecutor(node));
    }

    private String buildCycleMsg(IReliableTask<IFlowContext> task, IReliableTask<IFlowContext> parent) {

        StringBuilder builder = new StringBuilder("The relies of node in flow form a cycle:\n")
                .append("┌─────┐\n");
        if (!parent.equals(task)) {
            builder.append("|  ").append(task.getClass().getName()).append("\n")
                    .append("↑  ").append("   ↓").append("\n")
                    .append("|  ").append(parent.getClass().getName()).append("\n");
        } else {
            builder.append("↑  ").append(task.getClass().getName()).append("\n");
        }
        builder.append("└─────┘");
        return builder.toString();
    }

    private int computeAndGetNodeLevel(Node cur) {

        if (cur.getLevel() > Node.ROOT_LEVEL || ObjectUtils.isEmpty(cur.getParents())) {
            // 已计算过层级，或者没有父节点（即没有依赖任何特征）
            return cur.getLevel();
        }

        int parentMaxLevel = Node.ROOT_LEVEL;
        for (Node parent : cur.getParents()) {
            // 递归计算父节点（依赖节点）的最大层级
            parentMaxLevel = Math.max(computeAndGetNodeLevel(parent), parentMaxLevel);
        }

        // 每一层只比最大父层级大1，因此节点的层级在父节点的层级基础上加1
        cur.setLevel(parentMaxLevel + 1);
        return cur.getLevel();
    }

}
