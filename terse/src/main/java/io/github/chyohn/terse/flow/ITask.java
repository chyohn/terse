package io.github.chyohn.terse.flow;

/**
 * flow task
 *
 * @param <C> type Context
 * @author qiang.shao
 * @since 1.0.0
 */
public interface ITask<C extends IFlowContext> extends IReliableTask<C> {

    /**
     * provide the task handler who provide command and result handler
     *
     * @param c flow context
     * @return can return null if the task does nothing.
     */
    ITaskHandler createTaskHandler(C c);
}
