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

import io.github.chyohn.terse.flow.IFlowContext;
import lombok.Getter;
import lombok.Setter;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.command.ICommandX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
public class TerseFlowWithCallableCommandTest {

    @Test
    public void testFlow() throws InterruptedException {
        Terse.withReceiverBuilder(builder -> {
            builder.onReceive(SayHelloWorldCommand.class, (SayHelloWorldCommand command) -> {
                System.out.println("say hello world, tid: " + Thread.currentThread().getName());
                return "hello world";
            });
        });
        Terse.<FlowContext>flow()
                .callable("", context -> {
                    System.out.println("get hello, tid: " + Thread.currentThread().getName());
                    return "hello";
                }, (context, v) -> {
                    Assertions.assertEquals(v, "hello");
                    System.out.println("set hello, tid: " + Thread.currentThread().getName());
                    context.setHello(v);
                })
                .then()
                .then()
                .then()
                .then()
                .callable("", (context) -> {
                    System.out.println("get world, tid: " + Thread.currentThread().getName());
                    return "world";
                }, (context, v) -> {
                    Assertions.assertEquals(v, "world");
                    System.out.println("set world, tid: " + Thread.currentThread().getName());
                    context.setWorld(v);
                })
                .runCommands(context -> {
                    List<ICommandX<String>> commands = new ArrayList<>();
                    commands.add(new SayHelloWorldCommand());
                    return commands;
                }, (context, result, finished) -> {
                    System.out.println("set all, tid: " + Thread.currentThread().getName());
                    Assertions.assertEquals(result.getValue(), "hello world");
                    context.setAll(result.getValue());
                })
                .runCommand(context -> {
                    return new SayHelloWorldCommand();
                }, (context, result) -> {
                    System.out.println("set all, tid: " + Thread.currentThread().getName());
                    Assertions.assertEquals(result, "hello world");
                    context.setAll(result);
                })
                .then()
                .then()
                .then()
                .build(context -> {
                    Assertions.assertEquals(context.getHello(), "hello");
                    Assertions.assertEquals(context.getWorld(), "world");
                    Assertions.assertEquals(context.getAll(), "hello world");
                    System.out.println(context.getHello() + " " + context.getWorld() + ", all: " + context.getAll());
                })
                .execute(new FlowContext());


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

}
