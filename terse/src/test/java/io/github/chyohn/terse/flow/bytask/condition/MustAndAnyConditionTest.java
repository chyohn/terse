package io.github.chyohn.terse.flow.bytask.condition;

import io.github.chyohn.terse.MyCondition;
import io.github.chyohn.terse.NoRely;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.example.cal.CalculateCommand;
import io.github.chyohn.terse.example.cal.Op;
import io.github.chyohn.terse.exception.FlowConditionNotMatchException;
import io.github.chyohn.terse.flow.*;
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * (A+B)*(E/(C-D))
 * H1 = A + B
 * H2 = C - D
 * H3 = E/H2 WHEN H2 != 0
 * H4 = H1 * H3 when h2 > 0 or h1 > 0
 */
public class MustAndAnyConditionTest {

    @Test
    void testMustAndAnyCondition() {
        // must condition H3 = E/H2 WHEN H2 != 0
        Context c = new Context();
        c.setA(1);
        c.setB(2);
        c.setC(3);
        c.setD(3);
        c.setE(6);
        Assertions.assertThrows(FlowConditionNotMatchException.class, () -> {
            Terse.execute(new Summary(), c);
        });

        // H4 = H1 * H3 when h2 > 0 or h1 > 0
        // 两个条件都不满足
        c.setB(-2);
        c.setD(4);
        c.clear();
        Assertions.assertThrows(FlowConditionNotMatchException.class, () -> {
            Terse.execute(new Summary(), c);
        });
        // 都满足
        c.setB(2);
        c.setD(2);
        c.clear();
        Terse.execute(new Summary(), c);

        // 满足H2>0
        c.setB(-2);
        c.setD(2);
        c.clear();
        Terse.execute(new Summary(), c);
        // 满足H1>0
        c.setB(2);
        c.setD(4);
        c.clear();
        Terse.execute(new Summary(), c);
//        Assertions.assertNull(c.getH3());
    }

}

class Summary implements ISummaryTask<Context>, NoRely<Context> {

    @Override
    public void summary(Context context) {
        System.out.println("H1：" + context.getH1());
        System.out.println("H2：" + context.getH2());
        System.out.println("H3：" + context.getH3());
        System.out.println("H4：" + context.getH4());
    }

    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return Collections.singletonList(H4.INST);
    }
}

class H1 implements ITask<Context>, NoRely<Context> {
    static final H1 INST = new H1();
    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getA(), Op.ADD, c.getB()), c::setH1);
    }
    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return null;
    }
}


class H2 implements ITask<Context>, NoRely<Context> {
    static final H2 INST = new H2();
    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getC(), Op.SUBTRACT, c.getD()), c::setH2);
    }
    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return null;
    }
}


class H3 implements ITask<Context>, NoRely<Context> {
    static final H3 INST = new H3();
    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getE(), Op.DIVIDE, c.getH2()), c::setH3);
    }
    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return Collections.singletonList(H2.INST);
    }

    @Override
    public List<IConditionTask<Context>> mustConditions() {
        return Collections.singletonList(new MyCondition<Context>() {
            @Override
            public boolean isTrue(Context context) {
                return context.getH2() != 0;
            }

            @Override
            public List<ITask<Context>> mustRelyOnTasks() {
                return Collections.singletonList(H2.INST);
            }
        });
    }
}

class H4 implements ITask<Context>, NoRely<Context> {
    static final H4 INST = new H4();
    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getH1(), Op.MULTIPLY, c.getH3()), c::setH4);
    }
    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return Arrays.asList(H3.INST, H1.INST);
    }

    @Override
    public List<IConditionTask<Context>> randomConditions() {
        IConditionTask<Context> h1Gt0 = new MyCondition<Context>() {
            @Override
            public boolean isTrue(Context context) {
                return context.getH1() > 0;
            }

            @Override
            public List<ITask<Context>> mustRelyOnTasks() {
                return Collections.singletonList(H1.INST);
            }
        };

        IConditionTask<Context> h2Gt0 = new MyCondition<Context>() {
            @Override
            public boolean isTrue(Context context) {
                return context.getH2() > 0;
            }

            @Override
            public List<ITask<Context>> mustRelyOnTasks() {
                return Collections.singletonList(H2.INST);
            }
        };

        return Arrays.asList(h1Gt0, h2Gt0);
    }
}

@Getter
@Setter
class Context implements IFlowContext {
    int a;
    int b;
    int c;
    int d;
    int e;

    Integer h1;
    Integer h2;
    Integer h3;
    Integer h4;
    void clear() {
        h1 = h2 = h3 = h4 = null;
    }
}