package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.ITask;
import io.github.chyohn.terse.flow.ITaskHandler;
import io.github.chyohn.terse.utils.ObjectUtils;

import java.util.List;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class TaskNodeExecutor implements NodeExecutor{

    @Override
    public boolean accept(Node node) {
        return node.getTask() instanceof ITask;
    }

    @Override
    public List<ICommand> run(Node node, IFlowContext context) {
        ITask<IFlowContext> task = (ITask<IFlowContext>)node.getTask();
        ITaskHandler handler = task.createTaskHandler(context);
        if (handler == null) {
            return null;
        }

        List<ICommand> commands = handler.getCommand();
        if (ObjectUtils.isEmpty(commands)) {
            return null;
        }

        NodeExecuteInfo executeInfo = new NodeExecuteInfo(handler, commands.size());
        node.setExecuteInfo(executeInfo);
        return commands;
    }

    @Override
    public boolean handleResult(Node node, IResult<?> result) {
        NodeExecuteInfo executeInfo = node.getExecuteInfo();
        if (result == null && executeInfo == null) {
            return true;
        }

        executeInfo.incrFinish();
        boolean finished = executeInfo.isAllFinished();
        executeInfo.handler.handleResult(result, finished);
        return finished;
    }
}
