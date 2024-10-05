package io.github.chyohn.terse.flow;

import java.util.List;


/**
 * indicate the task's executable conditions
 *
 * @param <C> context
 * @author qiang.shao
 * @since 1.0.0
 */
public interface IReliableTask<C extends IFlowContext> {

    /**
     * provide current task execute rely on which tasks, and the task can execute after all the tasks are finished.
     *
     * @return can return null if don't have task to rely on
     */
    List<ITask<C>> mustRelyOnTasks();


    /**
     * provide current task execute rely on which tasks, and the task can execute after arbitrary one and all the must tasks are finished.
     *
     * @return can return null if don't have task to rely on
     */
    List<ITask<C>> randomRelyOnTasks();

    /**
     * Provide the conditions for executing the current task, which can only be executed if all conditions are met
     *
     * @return null is mean: unconditional execution
     */
    List<IConditionTask<C>> mustConditions();

    /**
     * Provide the conditions for executing the current task, which can be executed if arbitrary condition and all must conditions are met
     *
     * @return can return null if don't have conditional task to rely on
     */
    List<IConditionTask<C>> randomConditions();


    int SORT_FIRST = 1;
    int SORT_NORMAL = 2;
    int SORT_LAST = 3;

    /**
     * get the sort value
     *
     * @return sort index
     */
    default int sorted() {
        return SORT_NORMAL;
    }


    /**
     * get the time out of task
     *
     * @return time out millis.&lt;=0 no time out, wait forever.
     */
    default long getTimeout() {
        return -1;
    }
}
