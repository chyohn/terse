package io.github.chyohn.terse.flow.impl;

import java.util.Arrays;
import java.util.List;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class NodeExecutorFactory {
    private static final List<NodeExecutor> nodeExecutors = Arrays.asList(new TaskNodeExecutor(), new ConditionNodeExecutor(), new SummaryNodeExecutor());

    static NodeExecutor getExecutor(Node node) {
        for (NodeExecutor exe : nodeExecutors) {
            if (exe.accept(node)) {
                return exe;
            }
        }
        throw new NullPointerException("can't find out the executor for  type: " + node.getTask().getClass().getName());
    }
}
