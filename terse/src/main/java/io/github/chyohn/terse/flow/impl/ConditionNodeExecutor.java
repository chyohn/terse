package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.exception.FlowConditionNotMatchException;
import io.github.chyohn.terse.flow.IConditionTask;
import io.github.chyohn.terse.flow.IFlowContext;

import java.util.List;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class ConditionNodeExecutor implements NodeExecutor {
    @Override
    public boolean accept(Node node) {
        return node.getTask() instanceof IConditionTask;
    }

    @Override
    public List<ICommand> run(Node node, IFlowContext context) {

        IConditionTask<IFlowContext> condition = (IConditionTask<IFlowContext>) node.getTask();

        if (!condition.isTrue(context) && !removeChildren(node)) {
            throw new FlowConditionNotMatchException("flow can't match the condition.");
        }

        return null;
    }


    /**
     * remove children of node
     *
     * @param node the node
     * @return true remove success, false remove failure.
     */
    private boolean removeChildren(Node node) {

        if (node.isEndNode()) {
            return false;
        }

        for (Node child : node.getChildren()) {
            if (child.canRemove(node) && !removeChildren(child)) {
                return false;
            }
        }
        return true;
    }

}
