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

package io.github.chyohn.terse.demo.spring.flow.bytask;

import java.util.List;

import io.github.chyohn.terse.demo.spring.NoRely;
import io.github.chyohn.terse.flow.ITaskSingle;
import io.github.chyohn.terse.command.ICommandX;
import org.junit.jupiter.api.Assertions;
import io.github.chyohn.terse.flow.ITask;
import org.springframework.stereotype.Component;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Component
public class GetWorldTask implements ITaskSingle<FlowContext, String>, NoRely<FlowContext> {

    public String getText() {
        return "world";
    }

    @Override
    public ICommandX<String> getCommand(FlowContext context) {
        return new WordReceiverInitDependHelloReceiverHandler.WordRequest(getText());
    }

    @Override
    public void handleResult(FlowContext context, String result) {
        Assertions.assertEquals(getText(), result);
        System.out.println("set world, tid: " + Thread.currentThread().getName());
        context.setWorld(result);
    }

//    @Override
//    public ITaskHandler createTaskHandler(FlowContext context) {
//        String text = getText();
//        return TaskHandlerFactory.newTaskHandler(() -> {
//            List<ICommandX<String>> commands = new ArrayList<>();
//            commands.add(new WordReceiverInitDependHelloReceiverHandler.WordRequest(text));
//            return commands;
//        }, (v, finished) -> {
//            Assertions.assertEquals(text, v.getValue());
//            System.out.println("set world, tid: " + Thread.currentThread().getName());
//            context.setWorld(v.getValue());
//        });
//    }

    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }
}

