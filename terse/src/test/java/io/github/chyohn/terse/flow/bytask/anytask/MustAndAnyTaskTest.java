package io.github.chyohn.terse.flow.bytask.anytask;

import io.github.chyohn.terse.MyCondition;
import io.github.chyohn.terse.NoRely;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.example.cal.CalculateCommand;
import io.github.chyohn.terse.example.cal.Op;
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
 * (A+B)*(E/(C+D))
 * H1 = A + B
 * H2A = C - D when c > d
 * H2B = C + D when c <= d
 * H3 = E/H2A or E/H2B
 * H4 = H1 * H3
 */
public class MustAndAnyTaskTest {

    @Test
    void testMustAndAnyCondition() {
        // must condition H3 = E/H2 WHEN H2 != 0
        Context c = new Context();
        c.setA(1);
        c.setB(2);
        c.setD(3);
        c.setE(8);

        c.setC(9);
        Terse.execute(new Summary(), c);
        int h4 = c.getH4();

        System.out.println("--------------------");
        c.setC(3);
        Terse.execute(new Summary(), c);
        Assertions.assertEquals(h4, c.getH4());

        System.out.println("--------------------");
        c.setC(2);
        c.setD(4);
        Terse.execute(new Summary(), c);
        Assertions.assertEquals(h4, c.getH4());

        System.out.println("--------------------");
        c.setC(8);
        Terse.execute(new Summary(), c);
        Assertions.assertNotEquals(h4, c.getH4());
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


class H2A implements ITask<Context>, NoRely<Context> {
    static final H2A INST = new H2A();

    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getC(), Op.SUBTRACT, c.getD()), c::setH2);
    }

    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return null;
    }

    @Override
    public List<IConditionTask<Context>> mustConditions() {
        return Collections.singletonList(new MyCondition<Context>() {
            @Override
            public boolean isTrue(Context context) {
                return context.getC() > context.getD();
            }
        });
    }
}

class H2B implements ITask<Context>, NoRely<Context> {
    static final H2B INST = new H2B();

    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getC(), Op.ADD, c.getD()), c::setH2);
    }

    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return null;
    }

    @Override
    public List<IConditionTask<Context>> mustConditions() {
        return Collections.singletonList(new MyCondition<Context>() {
            @Override
            public List<ITask<Context>> mustRelyOnTasks() {
                return null;
            }

            @Override
            public boolean isTrue(Context context) {
                return context.getC() <= context.getD();
            }
        });
    }
}


class H3 implements ITask<Context>, NoRely<Context> {
    static final H3 INST = new H3();

    @Override
    public ITaskHandler createTaskHandler(Context c) {
        return TaskHandlerFactory.newTaskHandler(() -> new CalculateCommand(c.getE(), Op.MULTIPLY, c.getH2()), c::setH3);
    }

    @Override
    public List<ITask<Context>> mustRelyOnTasks() {
        return null;
    }

    @Override
    public List<ITask<Context>> randomRelyOnTasks() {
        return Arrays.asList(H2A.INST, H2B.INST);
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