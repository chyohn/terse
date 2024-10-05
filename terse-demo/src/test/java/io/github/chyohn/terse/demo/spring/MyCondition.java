package io.github.chyohn.terse.demo.spring;

import io.github.chyohn.terse.flow.IConditionTask;
import io.github.chyohn.terse.flow.IFlowContext;

public interface MyCondition<C extends IFlowContext> extends IConditionTask<C>, NoRely<C> {
}