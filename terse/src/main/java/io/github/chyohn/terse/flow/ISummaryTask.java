package io.github.chyohn.terse.flow;

/**
 * summary the task of flow, indicate the end of flow.
 *
 * @param <C> type Context
 * @author qiang.shao
 * @since 1.0.0
 */
public interface ISummaryTask<C extends IFlowContext> extends IReliableTask<C> {

    /**
     * summary the result after all the task is finished.
     *
     * @param c context
     */
    void summary(C c);

}
