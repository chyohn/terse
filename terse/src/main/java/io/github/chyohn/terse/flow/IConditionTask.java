package io.github.chyohn.terse.flow;


/**
 * define an condition task
 *
 * @param <C> type of context
 * @author qiang.shao
 * @since 1.0.0
 */
public interface IConditionTask<C extends IFlowContext> extends IReliableTask<C> {

    /**
     * check if the condition is met
     *
     * @param c context
     * @return true if condition is met
     */
    boolean isTrue(C c);

}
