package io.github.chyohn.terse;

import io.github.chyohn.terse.flow.IConditionTask;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IReliableTask;
import io.github.chyohn.terse.flow.ITask;

import java.util.List;

public interface NoRely<C extends IFlowContext> extends IReliableTask<C> {

    @Override
    default List<IConditionTask<C>> mustConditions(){return null;}

    @Override
    default List<ITask<C>> randomRelyOnTasks() {
        return null;
    }

    @Override
    default List<IConditionTask<C>> randomConditions() {
        return null;
    }

    @Override
    default List<ITask<C>> mustRelyOnTasks() {
        return null;
    }
}
