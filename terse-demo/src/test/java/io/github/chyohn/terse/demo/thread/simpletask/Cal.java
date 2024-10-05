package io.github.chyohn.terse.demo.thread.simpletask;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.demo.caculator.SimpleReliable;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.ISummaryTask;
import io.github.chyohn.terse.flow.ITask;
import io.github.chyohn.terse.flow.ITaskHandler;
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import static io.github.chyohn.terse.demo.thread.simpletask.Cal.MY_POOL;

public class Cal {
    public static final String MY_POOL = "my_pool";
    static {
        Terse.registerExecutor(MY_POOL, Executors.newFixedThreadPool(2));
    }


}

class H1 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H1 INSTANCE = new H1();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
           return context.getA() + context.getB();
        }, v -> {
            context.setH1(v);
        });
    }
}


class H2 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H2 INSTANCE = new H2();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getC() + context.getD();
        }, v -> {
            context.setH2(v);
        });
    }
}

class H3 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H3 INSTANCE = new H3();
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getE() - context.getF();
        }, v -> {
            context.setH3(v);
        });
    }
}

class H4 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H4 INSTANCE = new H4();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return Arrays.asList(H1.INSTANCE, H2.INSTANCE);
    }
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getH1() * context.getH2();
        }, v -> {
            context.setH4(v);
        });
    }
}

class H5 implements ITask<FlowContext>, SimpleReliable<FlowContext> {
    static H5 INSTANCE = new H5();
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return Arrays.asList(H3.INSTANCE, H4.INSTANCE);
    }
    @Override
    public ITaskHandler createTaskHandler(FlowContext context) {
        return TaskHandlerFactory.newTaskHandler(MY_POOL, () -> {
            return context.getH4() / context.getH3();
        }, v -> {
            context.setH5(v);
        });
    }
}

class Summary implements ISummaryTask<FlowContext>, SimpleReliable<FlowContext> {
    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return Collections.singletonList(H5.INSTANCE);
    }
    @Override
    public void summary(FlowContext context) {
        // summary result
        System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
                context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
    }
}

@Getter
@Setter
class FlowContext implements IFlowContext {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;
    private int f;

    private int h1;
    private int h2;
    private int h3;
    private int h4;
    private int h5;
}