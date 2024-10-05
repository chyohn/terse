package io.github.chyohn.terse.flow.impl;

import io.github.chyohn.terse.NoRely;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.ITask;
import io.github.chyohn.terse.flow.ITaskHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskRingTest {

    @Test
    void testCycle() {
        // 自依赖
        Task4.INS.setRelies(Task4.INS);
        test(Task4.INS);

        Task4.INS.setRelies(Task3.INS);
        Task3.INS.setRelies(Task4.INS);
        test(Task4.INS);

        Task4.INS.setRelies(Task3.INS);
        Task3.INS.setRelies(Task2.INS);
        Task2.INS.setRelies(Task4.INS);
        test(Task4.INS);

        Task2.INS.setRelies(Task3.INS);
        test(Task4.INS);

        // 自依赖
        Task2.INS.setRelies(Task2.INS);
        test(Task4.INS);

        Task2.INS.setRelies(Task1.INS, Task3.INS);
        test(Task4.INS);
    }
    void test(ITask<MyContext> task) {

        Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                DagGraphGenerator.generateDAGByLevel(task);
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.assertTrue(e.getMessage().contains("The relies of tasks in flow form a cycle"));
                throw e;
            }
        });
    }

    static class Task1 extends Task {

        static Task1 INS = new Task1();
    }


    static class Task2 extends Task {
        static Task2 INS = new Task2();
    }

    static class Task3 extends Task {
        static Task3 INS = new Task3();
    }


    static class Task4 extends Task {
        static Task4 INS = new Task4();
    }

    static class MyContext implements IFlowContext {

    }

    static abstract class Task implements ITask<MyContext>, NoRely<MyContext> {
        List<ITask<MyContext>> relies = new ArrayList<>();
        void setRelies(ITask<MyContext>... tasks) {
            if (tasks == null) {
                relies = null;
                return;
            }
            relies = Arrays.asList(tasks);
        }
        @Override
        public ITaskHandler createTaskHandler(MyContext context) {
            return null;
        }

        @Override
        public List<ITask<MyContext>> mustRelyOnTasks() {
            return relies;
        }
    }

}
