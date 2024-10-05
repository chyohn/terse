/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.flow;

import java.util.ArrayList;
import java.util.List;

import io.github.chyohn.terse.NoRely;
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import lombok.Getter;
import lombok.Setter;
import io.github.chyohn.terse.command.ICommandX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.command.IReceiverRegistry;
import io.github.chyohn.terse.spi.ISpiFactory;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
public class TaskImplTest {

    @Test
    public void testFlow() {

        IReceiverRegistry receiverManager = ISpiFactory.get(IReceiverRegistry.class);
        receiverManager.register(builder -> {
            builder.onReceive(SayHelloWorldCommand.class, (SayHelloWorldCommand command) -> {
                System.out.println("say hello world, tid: " + Thread.currentThread().getName());
                return "hello world";
            });
        });

        IFlowExecutor executor = ISpiFactory.get(IFlowExecutor.class);
        executor.execute(new SummaryTask(), new FlowContext(), RunningWay.PARALLEL);
    }

    private static class SayHelloWorldCommand implements ICommandX<String> {
        private int id;

        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(int id) {
            this.id = id;
        }
    }

    @Setter
    @Getter
    private static class FlowContext implements IFlowContext {

        private String hello;
        private String world;
        private String all;
    }

    private static class SummaryTask implements ISummaryTask<FlowContext>, NoRely<FlowContext> {

        @Override
        public void summary(FlowContext context) {
            Assertions.assertEquals(context.getHello(), "hello");
            Assertions.assertEquals(context.getWorld(), "world");
            Assertions.assertEquals(context.getAll(), "hello world");
            System.out.println(context.getHello() + " " + context.getWorld() + ", all: " + context.getAll());
        }

        @Override
        public List<ITask<FlowContext>> mustRelyOnTasks() {
            List<ITask<FlowContext>> list = new ArrayList<>();
            list.add(new GetHelloTask());
            list.add(new GetWorldTask());
            list.add(new GetAllWorldTask());
            return list;
        }
    }

    private static class GetAllWorldTask implements ITaskSingle<FlowContext, String>, NoRely<FlowContext> {

        @Override
        public ICommandX<String> getCommand(FlowContext context) {
            return new SayHelloWorldCommand();
        }

        @Override
        public void handleResult(FlowContext context, String result) {
            System.out.println("set all, tid: " + Thread.currentThread().getName());
            Assertions.assertEquals(result, "hello world");
            context.setAll(result);
        }

//        @Override
//        public ITaskHandler createTaskHandler(FlowContext context) {
//            return TaskHandlerFactory.newTaskHandler(() -> new SayHelloWorldCommand(), (r) -> {
//                System.out.println("set all, tid: " + Thread.currentThread().getName());
//                Assertions.assertEquals(r, "hello world");
//                context.setAll(r);
//            });
//        }

        @Override
        public List<ITask<FlowContext>> mustRelyOnTasks() {
            return null;
        }
    }

    private static class GetHelloTask implements ITask<FlowContext>, NoRely<FlowContext> {

        @Override
        public ITaskHandler createTaskHandler(FlowContext context) {
            return TaskHandlerFactory.newTaskHandler("", () -> {
                System.out.println("get hello, tid: " + Thread.currentThread().getName());
                return "hello";
            }, v -> {
                Assertions.assertEquals(v, "hello");
                System.out.println("set hello, tid: " + Thread.currentThread().getName());
                context.setHello(v);
            });
        }

        @Override
        public List<ITask<FlowContext>> mustRelyOnTasks() {
            return null;
        }
    }

    private static class GetWorldTask implements ITask<FlowContext>, NoRely<FlowContext> {

        @Override
        public ITaskHandler createTaskHandler(FlowContext context) {
            return TaskHandlerFactory.newTaskHandler("", () -> {
                System.out.println("get world, tid: " + Thread.currentThread().getName());
                return "world";
            }, v -> {
                Assertions.assertEquals(v, "world");
                System.out.println("set world, tid: " + Thread.currentThread().getName());
                context.setWorld(v);
            });
        }

        @Override
        public List<ITask<FlowContext>> mustRelyOnTasks() {
            return null;
        }
    }

}
