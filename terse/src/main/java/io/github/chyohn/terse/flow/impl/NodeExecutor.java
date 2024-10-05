package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.flow.IFlowContext;

import java.util.List;


/**
 * @author qiang.shao
 * @since 1.0.0
 */
interface NodeExecutor {

    List<ICommand> run(Node node, IFlowContext context);

    boolean accept(Node node);

    /**
     *
     * @return true finished node
     */
    default boolean handleResult(Node node, IResult<?> result) {
        return true;
    }
}
