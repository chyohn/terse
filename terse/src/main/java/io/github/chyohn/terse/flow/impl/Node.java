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
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IReliableTask;
import io.github.chyohn.terse.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Getter
class Node {
    final static int ROOT_LEVEL = 0;

    private final IReliableTask<IFlowContext> task; // 该节点的任务对象
    @Setter
    private int level = ROOT_LEVEL; // 层级，最顶层为0
    private Set<Node> parents; // 上层节点为父节点
    private final List<Node> children = new ArrayList<>(); // 下层节点为子节点
    private int childrenMinLevel = -1;
    private boolean removed;
    private Set<Node> mustRelyOnTasks;
    private Set<Node> randomRelyOnTasks;
    private Set<Node> mustConditions;
    private Set<Node> randomConditions;
    @Setter
    private NodeExecutor executor;
    @Setter
    private NodeExecuteInfo executeInfo;


    public Node(IReliableTask<IFlowContext> task) {
        this.task = task;
    }

    public void addMustRelyOnTasks(Node parent) {
        if (mustRelyOnTasks == null) {
            mustRelyOnTasks = new HashSet<>();
        }
        mustRelyOnTasks.add(parent);
        addParent(parent);
    }


    public void addRandomRelyOnTasks(Node parent) {
        if (randomRelyOnTasks == null) {
            randomRelyOnTasks = new HashSet<>();
        }
        randomRelyOnTasks.add(parent);
        addParent(parent);
    }

    public void addMustConditions(Node parent) {
        if (mustConditions == null) {
            mustConditions = new HashSet<>();
        }
        mustConditions.add(parent);
        addParent(parent);
    }


    public void addRandomConditions(Node parent) {
        if (randomConditions == null) {
            randomConditions = new HashSet<>();
        }
        randomConditions.add(parent);
        addParent(parent);
    }

    private void addParent(Node parent) {
        if (parents == null) {
            parents = new HashSet<>();
        }
        if (parents.add(parent)) {
            parent.children.add(this);
        }
    }

    /**
     * 执行节点
     *
     * @param context 流程上下文
     * @return 异步命令，null则不需要发起异步请求
     */
    public List<ICommand> execute(IFlowContext context) {
        clean();
        return executor.run(this, context);
    }

    /**
     * 如果结果发起的是异步命令，当异步命令响应后通过该接口处理响应结果
     *
     * @param result 命令结果
     * @return true finished node
     */
    public boolean handleResult(IResult<?> result) {
        return executor.handleResult(this, result);
    }


    public boolean canExecute(Node parent) {
        if (removed) {
            return false;
        }
        parentFinished(parent);
        return ObjectUtils.isEmpty(mustRelyOnTasks) && randomRelyOnTasks == null
                && ObjectUtils.isEmpty(mustConditions) && randomConditions == null;
    }

    private void parentFinished(Node parent) {
        if (parents == null || !parents.remove(parent)) {
            // parents中没有该节点，不需要处理后续
            return;
        }
        if (mustConditions != null && mustConditions.remove(parent)) {
            return;
        }
        if (mustRelyOnTasks != null && mustRelyOnTasks.remove(parent)) {
            return;
        }

        // 任意一个完成，则移除其他任意节点
        if (ObjectUtils.contains(randomRelyOnTasks, parent)) {
            randomRelyOnTasks = null;
            return;
        }
        if (ObjectUtils.contains(randomConditions, parent)) {
            randomConditions = null;
        }
    }


    /**
     * 父节点被移除，检查当前节点是否需要被移除
     *
     * @param parent 被移除的父节点
     * @return true当前节点也需要被移除
     */
    public boolean canRemove(Node parent) {
        if (!removed && removeParent(parent)) {
            clean();
            return true;
        }
        return false;
    }

    /**
     * 父节点被移除，检查当前节点是否需要被移除
     *
     * @param parent 被移除的父节点
     * @return true当前节点也需要被移除
     */
    private boolean removeParent(Node parent) {
        if (parents == null || !parents.remove(parent)) {
            // parents中没有该节点，不需要处理后续
            return false;
        }
        if (mustConditions != null && mustConditions.remove(parent)) {
            return true;
        }
        if (mustRelyOnTasks != null && mustRelyOnTasks.remove(parent)) {
            return true;
        }

        if (randomConditions != null && randomConditions.remove(parent)) {
            return randomConditions.isEmpty();
        }
        if (randomRelyOnTasks != null && randomRelyOnTasks.remove(parent)) {
            return randomRelyOnTasks.isEmpty();
        }
        return false;
    }

    public int getChildrenMinLevel() { // 获取子节点中的最小层级数
        if (childrenMinLevel >= 0) {
            return childrenMinLevel;
        }

        return childrenMinLevel = children.isEmpty() ? Integer.MAX_VALUE
                : children.stream().flatMapToInt(a -> IntStream.of(a.level)).min().getAsInt();
    }

    /**
     * @return true当前节点为结束节点
     */
    public boolean isEndNode() {
        return children.isEmpty();
    }

    /**
     * 清理节点
     */
    private void clean() {
        removed = true;
        parents = null;
        mustRelyOnTasks = null;
        randomRelyOnTasks = null;
        mustConditions = null;
        randomConditions = null;
    }

    @Override
    public String toString() {
        return task.getClass().getSimpleName() + "(" + level + "->" + getChildrenMinLevel() + ")";
    }
}
