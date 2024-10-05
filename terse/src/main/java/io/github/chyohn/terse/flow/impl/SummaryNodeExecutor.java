package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.ISummaryTask;

import java.util.List;


/**
 * @author qiang.shao
 * @since 1.0.0
 */
class SummaryNodeExecutor implements NodeExecutor{
    @Override
    public boolean accept(Node node) {
        return node.getTask() instanceof ISummaryTask;
    }

    @Override
    public List<ICommand> run(Node node, IFlowContext context) {
        ISummaryTask<IFlowContext> summaryTask = (ISummaryTask<IFlowContext>)node.getTask();
        summaryTask.summary(context);
        return null;
    }
}
